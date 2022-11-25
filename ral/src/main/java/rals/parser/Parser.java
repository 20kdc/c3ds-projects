/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

import rals.code.*;
import rals.cond.*;
import rals.diag.SrcPos;
import rals.diag.SrcPosFile;
import rals.expr.*;
import rals.hcm.DummyHCMRecorder;
import rals.hcm.HCMIntents;
import rals.lex.*;
import rals.stmt.*;
import rals.types.*;

/**
 * Parser, but also discards any hope of this being an AST...
 */
public class Parser {
	private static IncludeParseContext newContext(IDocPath stdlib) throws IOException {
		IncludeParseContext ic = new IncludeParseContext(new DummyHCMRecorder(), true);
		ic.searchPaths.add(stdlib);
		findParseFile(ic, null, "std/compiler_helpers.ral", null);
		return ic;
	}
	public static IncludeParseContext run(IDocPath stdlib, String path) throws IOException {
		IncludeParseContext ic = newContext(stdlib);
		IDocPath init = new FileDocPath(new File(path));
		parseFileAt(ic, new SrcPosFile(null, init, path));
		return ic;
	}
	public static String runCPXConnTest(IDocPath stdlib) throws IOException {
		IncludeParseContext ic = newContext(stdlib);
		findParseFile(ic, null, "std/cpx_connection_test.ral", null);
		StringBuilder sb = new StringBuilder();
		Scripts scr = ic.module.resolve(ic.typeSystem, ic.diags, ic.hcm);
		scr.compileInstall(new OuterCompileContext(sb, ic.typeSystem, ic.diags, false));
		ic.diags.unwrap();
		return sb.toString();
	}

	/**
	 * Finds and parses a file.
	 * Does do the include sanity check.
	 */
	public static void findParseFile(IncludeParseContext ctx, IDocPath relTo, String inc, SrcPos incFrom) throws IOException {
		LinkedList<IDocPath> attempts = new LinkedList<>();
		// relative path
		if (relTo != null) {
			IDocPath f = relTo.getRelative(inc);
			if (f != null)
				attempts.add(f);
		}
		// search paths
		for (IDocPath sp : ctx.searchPaths) {
			IDocPath f = sp.getRelative(inc);
			if (f != null)
				attempts.add(f);
		}
		// now attempt them all
		for (IDocPath f : attempts) {
			if (!f.isFile())
				continue;
			parseFileAt(ctx, new SrcPosFile(incFrom, f, inc));
			return;
		}
		throw new RuntimeException("Ran out of search paths trying to find " + inc + " from " + relTo + ", tried: " + attempts);
	}

	/**
	 * Parses a file.
	 * Does do the include sanity check.
	 */
	public static void parseFileAt(IncludeParseContext ctx, SrcPosFile fileId) throws IOException {
		// It's critically important we do this because otherwise getParentFile returns null.
		if (ctx.included.contains(fileId.docPath))
			return;
		ctx.included.add(fileId.docPath);
		if (ctx.outputIncludesToErr)
			System.err.println("include: " + fileId.shortName);
		try (Reader r = fileId.docPath.open()) {
			parseFileInnards(ctx, fileId.docPath.getRelative(".."), fileId, r);
		}
	}

	/**
	 * Parses a file with the given parent (for includes), SrcPosFile (for errors), and input stream.
	 * Does NOT do the include sanity check.
	 */
	public static void parseFileInnards(IncludeParseContext ctx, IDocPath hereParent, SrcPosFile spf, Reader r) throws IOException {
		try {
			Lexer lx = new Lexer(spf, r, ctx.diags, ctx.hcm);
			InsideFileContext ifc = new InsideFileContext(ctx, lx);
			while (true) {
				Token tkn = lx.next();
				if (tkn == null)
					break;
				// Needs to catch include exceptions
				try {
					if (tkn.isKeyword("include")) {
						String str = ParserExpr.parseConstString(ifc);
						lx.requireNextKw(";");
						findParseFile(ctx, hereParent, str, tkn.lineNumber);
					} else if (tkn.isKeyword("addSearchPath")) {
						String str = ParserExpr.parseConstString(ifc);
						lx.requireNextKw(";");
						IDocPath possible = hereParent.getRelative(str);
						if (possible != null)
							ctx.searchPaths.add(possible);
					} else {
						parseDeclaration(ifc, tkn);
					}
				} catch (Exception ex) {
					ctx.diags.error(tkn.lineNumber, "exception in declaration: ", ex);
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("in file " + spf.shortName, ex);
		}
	}

	public static void parseDeclaration(InsideFileContext ifc, Token tkn) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;
		ScriptsUR m = ifc.module;
		if (tkn.isKeyword("class")) {
			String name = ParserType.parseTypeName(ifc);
			Token xtkn = lx.requireNext();
			if (xtkn.isKeyword("extends")) {
				lx.back();
				RALType rt = ts.byName(name);
				if (rt instanceof RALType.AgentClassifier) {
					parseExtendsClauses(ifc, (RALType.AgentClassifier) rt);
				} else {
					throw new RuntimeException("No class " + name);
				}
			} else {
				lx.back();
				int a = ParserExpr.parseConstInteger(ifc);
				int b = ParserExpr.parseConstInteger(ifc);
				int c = ParserExpr.parseConstInteger(ifc);
				RALType.Agent ag = ts.declareClass(new Classifier(a, b, c), name, lx.genDefInfo(tkn));
				parseExtendsClauses(ifc, ag);
			}
		} else if (tkn.isKeyword("interface")) {
			String name = ParserType.parseTypeName(ifc);
			RALType.Agent ag = ts.declareInterface(name, lx.genDefInfo(tkn));
			parseExtendsClauses(ifc, ag);
		} else if (tkn.isKeyword("typedef")) {
			String name = ParserType.parseTypeName(ifc);
			RALType ty = ParserType.parseType(ifc);
			lx.requireNextKw(";");
			ts.declareTypedef(name, ty, lx.genDefInfo(tkn));
		} else if (tkn.isKeyword("field")) {
			RALType fieldType = ParserType.parseType(ifc);
			String name = ParserType.parseTypeName(ifc);
			RALType rt = ts.byName(name);
			if (rt instanceof RALType.Agent) {
				lx.requireNextKw(".");
				String fieldName = lx.requireNextID();
				int ovSlot = ParserExpr.parseConstInteger(ifc);
				lx.requireNextKw(";");
				((RALType.Agent) rt).declareField(fieldName, fieldType, lx.genDefInfo(tkn), ovSlot);
			} else {
				throw new RuntimeException("No class/interface " + name);
			}
		} else if (tkn.isKeyword("message")) {
			String name = ParserType.parseTypeName(ifc);
			RALType rt = ts.byName(name);
			if (rt instanceof RALType.Agent) {
				// allow -> or :
				if (!lx.optNextKw("->"))
					lx.requireNextKw(":");
				String msgName = lx.requireNextID();
				int msgId = ParserExpr.parseConstInteger(ifc);
				((RALType.Agent) rt).declareMS(msgName, msgId, false);
				lx.requireNextKw(";");
			} else {
				throw new RuntimeException("No class/interface " + name);
			}
		} else if (tkn.isKeyword("script")) {
			String name = ParserType.parseTypeName(ifc);
			RALType typ = ts.byName(name);
			if (!(typ instanceof RALType.Agent))
				throw new RuntimeException("Scripts can only be defined on classes/interfaces, not " + name);
			RALType.Agent ac = (RALType.Agent) typ; 
			// Ok, so there are three forms here:
			// script A:B {}
			// script A 1 {}
			// script A:B 1;
			// : may also be ->
			// Express which form is being used by 3 variables.
			int scriptId;
			String msgName = null;
			RALStatementUR stmt = null;
			// ...
			Token possibleDivider = lx.requireNext();
			if (!(possibleDivider.isKeyword(":") || possibleDivider.isKeyword("->"))) {
				// Not providing a name so not declaring a message ID.
				lx.back();
				scriptId = ParserExpr.parseConstInteger(ifc);
				stmt = ParserCode.parseStatement(ifc);
			} else {
				msgName = lx.requireNextID();
				// If this form is followed by a { then we assume it to be a declaration.
				// Otherwise it's a constant integer.
				Token chk = lx.requireNext();
				boolean isDeclaration = !chk.isKeyword("{");
				lx.back();
				if (isDeclaration) {
					scriptId = ParserExpr.parseConstInteger(ifc);
					lx.requireNextKw(";");
				} else {
					Integer msgId = ac.lookupMSID(msgName, true);
					if (msgId == null)
						throw new RuntimeException("No such script ID: " + name + ":" + msgName);
					scriptId = msgId;
					stmt = ParserCode.parseStatement(ifc);
				}
			}
			if (stmt != null) {
				if (!(ac instanceof RALType.AgentClassifier))
					throw new RuntimeException("Scripts can only be implemented on classes.");
				RALType.AgentClassifier acl = (RALType.AgentClassifier) ac; 
				m.eventScripts.put(new ScriptIdentifier(acl.classifier, scriptId), stmt);
			} else {
				ac.declareMS(msgName, scriptId, true);
			}
		} else if (tkn.isKeyword("install")) {
			m.addInstall(ParserCode.parseStatement(ifc));
		} else if (tkn.isKeyword("remove")) {
			m.addRemove(ParserCode.parseStatement(ifc));
		} else if (tkn.isKeyword("macro")) {
			boolean isStmtMacro = lx.requireNext().isKeyword("(");
			lx.back();
			if (isStmtMacro) {
				MacroArg[] rets = parseArgList(ifc, false);
				ifc.hcm.addCompletionIntentToNextToken(HCMIntents.CALLABLE, true);
				String name = lx.requireNextID();
				MacroArg[] args = parseArgList(ifc, true);
				RALStatementUR rs = ParserCode.parseStatement(ifc);

				DefInfo.At range = lx.genDefInfo(tkn);
				// continue
				m.addMacro(name, args.length, new Macro(range, name, args, new RALStmtExprInverted(range, rets, rs)));
			} else {
				ifc.hcm.addCompletionIntentToNextToken(HCMIntents.CALLABLE, true);
				String name = lx.requireNextID();
				MacroArg[] args = parseArgList(ifc, true);
				RALExprUR rs = ParserExpr.parseExpr(ifc, false);

				DefInfo.At range = lx.genDefInfo(tkn);
				m.addMacro(name, args.length, new Macro(range, name, args, rs));
			}
		} else if (tkn.isKeyword("overrideOwnr")) {
			int scrId = ParserExpr.parseConstInteger(ifc);
			ts.overrideOwnr.put(scrId, ParserType.parseType(ifc));
			lx.requireNextKw(";");
		} else if (tkn.isKeyword("messageHook")) {
			int scrId = ParserExpr.parseConstInteger(ifc);
			ts.messageHooks.add(scrId);
			lx.requireNextKw(";");
		} else if (tkn.isKeyword("assertConst")) {
			RALConstant rc1 = ParserExpr.parseConst(ifc);
			if (!RALCondition.constToBool(rc1))
				throw new RuntimeException("failed constant assert at " + tkn.lineNumber);
		} else if (tkn.isKeyword(";")) {
			// :D
		} else if (tkn instanceof Token.ID) {
			String name = ((Token.ID) tkn).text;
			Token tx = lx.requireNext();
			if (!tx.isKeyword("=")) {
				lx.back();
				ifc.diags.error(tkn.lineNumber, "unknown declaration " + tkn);
			} else {
				RALConstant cst = ParserExpr.parseConst(ifc);
				lx.requireNextKw(";");
				ts.declareConst(name, lx.genDefInfo(tkn), cst);
			}
		} else {
			ifc.diags.error(tkn.lineNumber, "unknown declaration " + tkn);
		}
	}

	private static MacroArg[] parseArgList(InsideFileContext ifc, boolean allowInline) {
		Lexer lx = ifc.lexer;
		lx.requireNextKw("(");
		Token first = lx.requireNext();
		if (first.isKeyword(")"))
			return new MacroArg[0];
		lx.back();
		LinkedList<MacroArg> args = new LinkedList<>();
		while (true) {
			RALType typ = ParserType.parseType(ifc);
			boolean isInline = false;
			first = lx.requireNext();
			if (first.isKeyword("&")) {
				isInline = true;
			} else {
				lx.back();
			}
			String name = lx.requireNextID();
			args.add(new MacroArg(typ, isInline, name));
			first = lx.requireNext();
			if (first.isKeyword(")")) {
				return args.toArray(new MacroArg[0]);
			} else if (first.isKeyword(",")) {
				// okie-dokie
			} else {
				throw new RuntimeException("Unusual termination of argument list " + first);
			}
		}
	}

	public static void parseExtendsClauses(InsideFileContext ifc, RALType.Agent ag) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;
		while (true) {
			Token clause = lx.requireNext();
			if (clause.isKeyword(";"))
				break;
			if (clause.isKeyword("extends")) {
				while (true) {
					String other = ParserType.parseTypeName(ifc);
					RALType rt = ts.byName(other);
					if (rt instanceof RALType.Agent) {
						ag.addParent((RALType.Agent) rt);
					} else {
						throw new RuntimeException("extends clause requires '" + other + "' to be some form of agent");
					}
					Token nxt = lx.requireNext();
					if (!nxt.isKeyword(",")) {
						lx.back();
						break;
					}
				}
			}
		}
	}
}
