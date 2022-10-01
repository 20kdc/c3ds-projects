/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import rals.code.Macro;
import rals.code.MacroArg;
import rals.code.Module;
import rals.expr.RALConstant;
import rals.expr.RALExprUR;
import rals.expr.RALStmtExprInverted;
import rals.lex.Lexer;
import rals.lex.Token;
import rals.stmt.RALStatementUR;
import rals.types.AgentInterface;
import rals.types.Classifier;
import rals.types.RALType;
import rals.types.TypeSystem;
import rals.types.RALType.Agent;
import rals.types.ScriptIdentifier;

/**
 * Parser, but also discards any hope of this being an AST...
 */
public class Parser {
	public static void parseFile(TypeSystem ts, Module m, File[] searchPaths, String inc) throws IOException {
		for (File sp : searchPaths) {
			File f = new File(sp, inc);
			if (!f.exists())
				continue;
			try (FileInputStream fis = new FileInputStream(f)) {
				Lexer lx = new Lexer(f.getPath(), fis);
				while (true) {
					Token tkn = lx.next();
					if (tkn == null)
						break;
					if (tkn.isKeyword("include")) {
						String str = ParserExpr.parseConstString(ts, lx);
						lx.requireNextKw(";");
						try {
							parseFile(ts, m, searchPaths, str);
						} catch (Exception ex) {
							throw new RuntimeException("in included file " + str, ex);
						}
					} else {
						try {
							parseDeclaration(ts, m, tkn, lx);
						} catch (Exception ex) {
							throw new RuntimeException("declaration of " + tkn + " at line " + tkn.lineNumber, ex);
						}
					}
				}
			}
			return;
		}
		throw new RuntimeException("Ran out of search paths trying to find " + inc);
	}
	public static void parseDeclaration(TypeSystem ts, Module m, Token tkn, Lexer lx) {
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
		} else if (tkn.isKeyword("message")) {
			String name = lx.requireNextID();
			RALType rt = ts.byName(name);
			if (rt instanceof RALType.Agent) {
				lx.requireNextKw(":");
				String msgName = lx.requireNextID();
				int msgId = lx.requireNextInteger();
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
			// Express which form is being used by 3 variables.
			int scriptId;
			String msgName = null;
			RALStatementUR stmt = null;
			// ...
			if (!lx.requireNext().isKeyword(":")) {
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
						throw new RuntimeException("No such message ID: " + name + ":" + msgName);
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
			m.installScript = ParserCode.parseStatement(ts, lx);
		} else if (tkn.isKeyword("remove")) {
			m.removeScript = ParserCode.parseStatement(ts, lx);
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
		} else if (tkn.isKeyword(";")) {
			// :D
		} else if (tkn instanceof Token.ID) {
			String name = ((Token.ID) tkn).text;
			Token tx = lx.requireNext();
			if (!tx.isKeyword("="))
				throw new RuntimeException("unknown declaration " + tkn);
			RALConstant cst = ParserExpr.parseConst(ts, lx);
			ts.declareConst(name, cst);
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
			boolean isInline = false;
			first = lx.requireNext();
			if (first.isKeyword("inline")) {
				isInline = true;
			} else {
				lx.back();
			}
			RALType typ = ParserType.parseType(ts, lx);
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
					if (rt instanceof Agent) {
						ag.addParent((Agent) rt);
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
