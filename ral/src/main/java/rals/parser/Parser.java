/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import rals.Main;
import rals.code.*;
import rals.cond.*;
import rals.expr.*;
import rals.lex.*;
import rals.stmt.*;
import rals.types.*;

/**
 * Parser, but also discards any hope of this being an AST...
 */
public class Parser {
	public static IncludeParseContext run(File stdlib, String path) throws IOException {
		File init = new File(path);
		IncludeParseContext ic = new IncludeParseContext();
		ic.searchPaths.add(stdlib);
		findParseFile(ic, null, "std/compiler_helpers.ral");
		parseFileAt(ic, init, path);
		return ic;
	}
	public static String runCPXConnTest(File stdlib) throws IOException {
		IncludeParseContext ic = new IncludeParseContext();
		ic.searchPaths.add(stdlib);
		findParseFile(ic, null, "std/compiler_helpers.ral");
		findParseFile(ic, null, "std/cpx_connection_test.ral");
		StringBuilder sb = new StringBuilder();
		ic.module.compileInstall(sb, ic.typeSystem, false);
		return sb.toString();
	}

	/**
	 * Finds and parses a file.
	 * Does do the include sanity check.
	 */
	public static void findParseFile(IncludeParseContext ctx, File relTo, String inc) throws IOException {
		LinkedList<File> attempts = new LinkedList<>();
		// relative path
		if (relTo != null) {
			File f = new File(relTo, inc);
			attempts.add(f);
		}
		// search paths
		for (File sp : ctx.searchPaths) {
			File f = new File(sp, inc);
			attempts.add(f);
		}
		// now attempt them all
		for (File f : attempts) {
			if (!f.exists())
				continue;
			parseFileAt(ctx, f, inc);
			return;
		}
		throw new RuntimeException("Ran out of search paths trying to find " + inc + " from " + relTo + ", tried: " + attempts);
	}

	/**
	 * Parses a file.
	 * Does do the include sanity check.
	 */
	public static void parseFileAt(IncludeParseContext ctx, File here, String name) throws IOException {
		// It's critically important we do this because otherwise getParentFile returns null.
		here = here.getAbsoluteFile();
		if (ctx.included.contains(here))
			return;
		ctx.included.add(here);
		System.out.println("include: " + name);
		try (FileInputStream fis = new FileInputStream(here)) {
			parseFileInnards(ctx, here.getParentFile(), name, fis);
		}
	}

	/**
	 * Parses a file given it's parent (for includes), name (for errors), and input stream.
	 * Does NOT do the include sanity check.
	 */
	public static void parseFileInnards(IncludeParseContext ctx, File hereParent, String name, InputStream fis) throws IOException {
		try {
			Lexer lx = new Lexer(name, fis);
			while (true) {
				Token tkn = lx.next();
				if (tkn == null)
					break;
				if (tkn.isKeyword("include")) {
					String str = ParserExpr.parseConstString(ctx.typeSystem, lx);
					lx.requireNextKw(";");
					findParseFile(ctx, hereParent, str);
				} else if (tkn.isKeyword("addSearchPath")) {
					String str = ParserExpr.parseConstString(ctx.typeSystem, lx);
					lx.requireNextKw(";");
					ctx.searchPaths.add(new File(hereParent, str));
				} else {
					try {
						parseDeclaration(ctx.typeSystem, ctx.module, tkn, lx);
					} catch (Exception ex) {
						throw new RuntimeException("declaration of " + tkn + " at line " + tkn.lineNumber, ex);
					}
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("in file " + name, ex);
		}
	}

	public static void parseDeclaration(TypeSystem ts, Scripts m, Token tkn, Lexer lx) {
		if (tkn.isKeyword("class")) {
			String name = lx.requireNextID();
			Token xtkn = lx.requireNext();
			if (xtkn.isKeyword("extends")) {
				lx.back();
				RALType rt = ts.byName(name);
				if (rt instanceof RALType.AgentClassifier) {
					parseExtendsClauses(ts, (RALType.AgentClassifier) rt, lx);
				} else {
					throw new RuntimeException("No class " + name);
				}
			} else {
				lx.back();
				int a = ParserExpr.parseConstInteger(ts, lx);
				int b = ParserExpr.parseConstInteger(ts, lx);
				int c = ParserExpr.parseConstInteger(ts, lx);
				RALType.Agent ag = ts.declareClass(new Classifier(a, b, c), name);
				parseExtendsClauses(ts, ag, lx);
			}
		} else if (tkn.isKeyword("interface")) {
			String name = lx.requireNextID();
			RALType.Agent ag = ts.declareInterface(name);
			parseExtendsClauses(ts, ag, lx);
		} else if (tkn.isKeyword("typedef")) {
			String name = lx.requireNextID();
			ts.declareTypedef(name, ParserType.parseType(ts, lx));
			lx.requireNextKw(";");
		} else if (tkn.isKeyword("field")) {
			RALType fieldType = ParserType.parseType(ts, lx);
			String name = lx.requireNextID();
			RALType rt = ts.byName(name);
			if (rt instanceof RALType.Agent) {
				lx.requireNextKw(".");
				String fieldName = lx.requireNextID();
				int ovSlot = ParserExpr.parseConstInteger(ts, lx);
				((RALType.Agent) rt).declareField(fieldName, fieldType, ovSlot);
				lx.requireNextKw(";");
			} else {
				throw new RuntimeException("No class/interface " + name);
			}
		} else if (tkn.isKeyword("message")) {
			String name = lx.requireNextID();
			RALType rt = ts.byName(name);
			if (rt instanceof RALType.Agent) {
				// allow -> or :
				if (!lx.optNextKw("->"))
					lx.requireNextKw(":");
				String msgName = lx.requireNextID();
				int msgId = ParserExpr.parseConstInteger(ts, lx);
				((RALType.Agent) rt).declareMS(msgName, msgId, false);
				lx.requireNextKw(";");
			} else {
				throw new RuntimeException("No class/interface " + name);
			}
		} else if (tkn.isKeyword("script")) {
			String name = lx.requireNextID();
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
				scriptId = ParserExpr.parseConstInteger(ts, lx);
				stmt = ParserCode.parseStatement(ts, lx);
			} else {
				msgName = lx.requireNextID();
				// If this form is followed by a { then we assume it to be a declaration.
				// Otherwise it's a constant integer.
				Token chk = lx.requireNext();
				boolean isDeclaration = !chk.isKeyword("{");
				lx.back();
				if (isDeclaration) {
					scriptId = ParserExpr.parseConstInteger(ts, lx);
					lx.requireNextKw(";");
				} else {
					Integer msgId = ac.lookupMSID(msgName, true);
					if (msgId == null)
						throw new RuntimeException("No such script ID: " + name + ":" + msgName);
					scriptId = msgId;
					stmt = ParserCode.parseStatement(ts, lx);
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
			m.addInstall(ParserCode.parseStatement(ts, lx));
		} else if (tkn.isKeyword("remove")) {
			m.addRemove(ParserCode.parseStatement(ts, lx));
		} else if (tkn.isKeyword("macro")) {
			boolean isStmtMacro = lx.requireNext().isKeyword("(");
			lx.back();
			if (isStmtMacro) {
				MacroArg[] rets = parseArgList(ts, lx, false);
				String name = lx.requireNextID();
				MacroArg[] args = parseArgList(ts, lx, true);
				RALStatementUR rs = ParserCode.parseStatement(ts, lx);
				m.addMacro(name, args.length, new Macro(name, args, new RALStmtExprInverted(rets, rs)));
			} else {
				String name = lx.requireNextID();
				MacroArg[] args = parseArgList(ts, lx, true);
				RALExprUR rs = ParserExpr.parseExpr(ts, lx, false);
				m.addMacro(name, args.length, new Macro(name, args, rs));
			}
		} else if (tkn.isKeyword("overrideOwnr")) {
			int scrId = ParserExpr.parseConstInteger(ts, lx);
			ts.overrideOwnr.put(scrId, ParserType.parseType(ts, lx));
			lx.requireNextKw(";");
		} else if (tkn.isKeyword("messageHook")) {
			int scrId = ParserExpr.parseConstInteger(ts, lx);
			ts.messageHooks.add(scrId);
			lx.requireNextKw(";");
		} else if (tkn.isKeyword("assertConst")) {
			RALConstant rc1 = ParserExpr.parseConst(ts, lx);
			if (!RALCondition.constToBool(rc1))
				throw new RuntimeException("failed constant assert at " + tkn.lineNumber);
		} else if (tkn.isKeyword(";")) {
			// :D
		} else if (tkn instanceof Token.ID) {
			String name = ((Token.ID) tkn).text;
			Token tx = lx.requireNext();
			if (!tx.isKeyword("="))
				throw new RuntimeException("unknown declaration " + tkn);
			RALConstant cst = ParserExpr.parseConst(ts, lx);
			ts.declareConst(name, tkn.lineNumber, cst);
			if (!lx.requireNext().isKeyword(";"))
				throw new RuntimeException("constant termination weirdness");
		} else {
			throw new RuntimeException("unknown declaration " + tkn);
		}
	}

	private static MacroArg[] parseArgList(TypeSystem ts, Lexer lx, boolean allowInline) {
		lx.requireNextKw("(");
		Token first = lx.requireNext();
		if (first.isKeyword(")"))
			return new MacroArg[0];
		lx.back();
		LinkedList<MacroArg> args = new LinkedList<>();
		while (true) {
			RALType typ = ParserType.parseType(ts, lx);
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

	public static void parseExtendsClauses(TypeSystem ts, RALType.Agent ag, Lexer lx) {
		while (true) {
			Token clause = lx.requireNext();
			if (clause.isKeyword(";"))
				break;
			if (clause.isKeyword("extends")) {
				while (true) {
					String other = lx.requireNextID();
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
