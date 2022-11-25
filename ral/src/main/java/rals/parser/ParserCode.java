/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.LinkedList;

import rals.diag.SrcPos;
import rals.expr.*;
import rals.expr.RALChainOp.Op;
import rals.lex.*;
import rals.lex.Token.StrEmb;
import rals.stmt.*;
import rals.types.*;

/**
 * Code parser.
 */
public class ParserCode {
	/**
	 * Beware: Call stmtCompletionIntents if doing, say, "}"-checking
	 */
	public static RALStatementUR parseStatement(InsideFileContext ifc) {
		stmtCompletionIntents(ifc);
		Token tkn = ifc.lexer.requireNext();
		try {
			return parseStatementInnards(ifc, tkn);
		} catch (Exception ex) {
			ifc.diags.error(tkn.lineNumber, "exception in statement: ", ex);
			return new RALBlock(tkn.lineNumber, false);
		}
	}
	private static RALStatementUR parseStatementInnards(InsideFileContext ifc, Token tkn) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;
		if (tkn.isKeyword("{")) {
			RALBlock rb = new RALBlock(tkn.lineNumber, true);
			while (true) {
				stmtCompletionIntents(ifc);
				tkn = lx.requireNext();
				if (tkn.isKeyword("}"))
					break;
				lx.back();
				rb.content.add(parseStatement(ifc));
			}
			return rb;
		} else if (tkn.isKeyword("&")) {
			return new RALInlineStatement(tkn.lineNumber, parseStringEmbed(ifc, false));
		} else if (tkn.isKeyword("let")) {
			return parseLetStatement(tkn.lineNumber, ifc);
		} else if (tkn.isKeyword("alias")) {
			Token.ID id = lx.requireNextIDTkn();
			if (lx.optNextKw("=")) {
				// alias x = y;
				RALExprUR res = ParserExpr.parseExpr(ifc, true);
				lx.requireNextKw(";");
				return new RALAliasStatement(tkn.lineNumber, id.text, res);
			} else if (lx.optNextKw("!")) {
				// alias x!Y;
				RALType rt = ParserType.parseType(ifc);
				lx.requireNextKw(";");
				return new RALAliasStatement(tkn.lineNumber, id.text, RALCast.of(new RALAmbiguousID(id.extent, ts, id.text), rt));
			} else {
				throw new RuntimeException("Did not understand alias form at: " + tkn);
			}
		} else if (tkn.isKeyword("if")) {
			RALExprUR cond = ParserExpr.parseExpr(ifc, true);
			RALStatementUR body = ParserCode.parseStatement(ifc);
			RALStatementUR elseBranch = null;
			Token chk = lx.requireNext();
			if (chk.isKeyword("else")) {
				elseBranch = ParserCode.parseStatement(ifc);
			} else {
				lx.back();
			}
			return new RALIfStatement(tkn.lineNumber, cond, body, elseBranch, false);
		} else if (tkn.isKeyword("while")) {
			RALExprUR cond = ParserExpr.parseExpr(ifc, true);
			RALStatementUR body = ParserCode.parseStatement(ifc);
			RALBlock outerBlock = new RALBlock(tkn.lineNumber, true);
			outerBlock.content.add(new RALIfStatement(tkn.lineNumber, cond, new RALBreakFromLoop(tkn.lineNumber), null, true));
			outerBlock.content.add(body);
			return new RALBreakableLoop(tkn.lineNumber, outerBlock);
		} else if (tkn.isKeyword("for")) {
			RALStatementUR init = parseLetStatement(tkn.lineNumber, ifc);
			RALExprUR cond = ParserExpr.parseExpr(ifc, true);
			lx.requireNextKw(";");
			RALStatementUR adjust = parseStatement(ifc);
			RALStatementUR body = parseStatement(ifc);
			RALBlock outerBlock = new RALBlock(tkn.lineNumber, true);
			RALBlock innerBlock = new RALBlock(tkn.lineNumber, false);
			innerBlock.content.add(new RALIfStatement(tkn.lineNumber, cond, new RALBreakFromLoop(tkn.lineNumber), null, true));
			innerBlock.content.add(body);
			innerBlock.content.add(adjust);
			// Actual structure is: {
			//  init
			//  loop {
			//   if !cond break;
			//   body
			//   adjust
			//  }
			// }
			outerBlock.content.add(init);
			outerBlock.content.add(new RALBreakableLoop(tkn.lineNumber, innerBlock));
			return outerBlock;
		} else if (tkn.isKeyword("break")) {
			lx.requireNextKw(";");
			return new RALBreakFromLoop(tkn.lineNumber);
		} else if (tkn.isKeyword("foreach")) {
			lx.requireNextKw("("); // for flexibility in syntax
			RALType iterOver = ParserType.parseType(ifc);
			lx.requireNextKw("in");
			String subType = lx.requireNextID();
			RALExprUR params = ParserExpr.parseExpr(ifc, false);
			lx.requireNextKw(")");
			RALStatementUR body = ParserCode.parseStatement(ifc);
			return new RALEnumLoop(tkn.lineNumber, iterOver, subType, params, body);
		} else if (tkn.isKeyword("with")) {
			boolean paren = lx.optNextKw("(");
			RALType type = ParserType.parseType(ifc);
			if (!(type instanceof RALType.AgentClassifier))
				throw new RuntimeException("Can only 'with' on classes");
			Classifier cl = ((RALType.AgentClassifier) type).classifier;
			Token.ID varName = lx.requireNextIDTkn();
			RALExprUR var = new RALAmbiguousID(varName.extent, ts, varName.text);
			if (paren)
				lx.requireNextKw(")");
			RALStatementUR body = ParserCode.parseStatement(ifc);
			RALStatementUR elseBranch = null;
			Token chk = lx.requireNext();
			if (chk.isKeyword("else")) {
				elseBranch = ParserCode.parseStatement(ifc);
			} else {
				lx.back();
			}
			RALBlock bodyOuter = new RALBlock(tkn.lineNumber, true);
			bodyOuter.content.add(new RALAliasStatement(tkn.lineNumber, varName.text, RALCast.of(var, type)));
			bodyOuter.content.add(body);
			return new RALIfStatement(tkn.lineNumber, new RALInstanceof(cl, var), bodyOuter, elseBranch, false);
		} else if (tkn.isKeyword("call")) {
			// CALL
			RALExprUR getMsgType = parseRelativeMessageID(new RALAmbiguousID(tkn.lineNumber.toRange(), ts, "ownr"), ifc);
			lx.requireNextKw("(");
			RALExprUR params = ParserExpr.parseExpr(ifc, false);
			lx.requireNextKw(")");
			lx.requireNextKw(";");
			RALCall call = new RALCall("__ral_compiler_helper_call", RALExprGroupUR.of(getMsgType, params));
			return new RALAssignStatement(tkn.lineNumber, null, call);
		} else {
			lx.back();
			// System.out.println("entered expr parser with " + tkn);
			RALExprUR target = ParserExpr.parseExpr(ifc, true);
			Token sp = lx.requireNext();
			if (sp.isKeyword(";")) {
				return new RALAssignStatement(tkn.lineNumber, null, target);
			} else if (sp.isKeyword("=")) {
				RALExprUR source = ParserExpr.parseExpr(ifc, true);
				lx.requireNextKw(";");
				return new RALAssignStatement(tkn.lineNumber, target, source);
			} else if (sp.isKeyword("+=")) {
				return parseModAssign(tkn.lineNumber, ifc, target, RALChainOp.ADD);
			} else if (sp.isKeyword("-=")) {
				return parseModAssign(tkn.lineNumber, ifc, target, RALChainOp.SUB);
			} else if (sp.isKeyword("*=")) {
				return parseModAssign(tkn.lineNumber, ifc, target, RALChainOp.MUL);
			} else if (sp.isKeyword("/=")) {
				return parseModAssign(tkn.lineNumber, ifc, target, RALChainOp.DIV);
			} else if (sp.isKeyword("|=")) {
				return parseModAssign(tkn.lineNumber, ifc, target, RALChainOp.OR);
			} else if (sp.isKeyword("&=")) {
				return parseModAssign(tkn.lineNumber, ifc, target, RALChainOp.AND);
			} else if (sp.isKeyword("->")) {
				// MESG WRT+
				RALExprUR getMsgType = parseRelativeMessageID(target, ifc);
				lx.requireNextKw("(");
				RALExprUR params = ParserExpr.parseExpr(ifc, false);
				lx.requireNextKw(")");
				RALExprUR after = null;
				if (lx.optNextKw("after"))
					after = ParserExpr.parseExpr(ifc, true);
				lx.requireNextKw(";");
				RALCall call;
				if (after == null) {
					call = new RALCall("__ral_compiler_helper_emit_na", RALExprGroupUR.of(target, getMsgType, params));
				} else {
					call = new RALCall("__ral_compiler_helper_emit", RALExprGroupUR.of(target, getMsgType, after, params));
				}
				return new RALAssignStatement(tkn.lineNumber, null, call);
			} else {
				ifc.diags.error(sp.lineNumber, sp + " after expression statement");
				return new RALBlock(tkn.lineNumber, false);
			}
		}
	}

	private static RALExprUR parseRelativeMessageID(RALExprUR target, InsideFileContext ifc) {
		Token messageId = ifc.lexer.requireNext();
		RALExprUR getMsgType;
		if (messageId instanceof Token.ID) {
			getMsgType = new RALMessageIDGrabber(target, ((Token.ID) messageId).text);
		} else {
			ifc.lexer.back();
			getMsgType = ParserExpr.parseExpr(ifc, true);
		}
		return getMsgType;
	}

	private static RALStatementUR parseModAssign(SrcPos lineNumber, InsideFileContext ifc, RALExprUR target, Op add) {
		RALExprUR source = ParserExpr.parseExpr(ifc, true);
		ifc.lexer.requireNextKw(";");
		return new RALAssignStatement(lineNumber, target, RALChainOp.of(target, add, source));
	}

	private static RALStatementUR parseLetStatement(SrcPos lineNumber, InsideFileContext ifc) {
		Lexer lx = ifc.lexer;

		LinkedList<String> names = new LinkedList<>();
		LinkedList<RALType> types = new LinkedList<>();
		RALExprUR re = null;
		if (lx.optNextKw(";")) {
			// "let;" - block exists solely to be empty
			// also used for "for" without initializers
			return new RALBlock(lineNumber, false);
		}
		boolean hasAnyAuto = false;
		while (true) {
			// work out if this is auto-typed
			lx.requireNext();
			Token tmp2 = lx.requireNext();
			// Note we don't allow auto-typed variables to even parse if there's no assignment
			boolean isAuto = tmp2.isKeyword("=") || tmp2.isKeyword(",");
			// Now go back on this and reparse
			lx.back();
			lx.back();
			// Ok
			RALType rt;
			String n;
			if (isAuto) {
				rt = null;
				n = lx.requireNextID();
				hasAnyAuto = true;
			} else {
				rt = ParserType.parseType(ifc);
				n = lx.requireNextID();
			}
			// confirmed!
			names.add(n);
			types.add(rt);
			Token chk = lx.requireNext();
			if (chk.isKeyword("=")) {
				re = ParserExpr.parseExpr(ifc, true);
				lx.requireNextKw(";");
				break;
			} else if (chk.isKeyword(";")) {
				break;
			} else if (!chk.isKeyword(",")) {
				throw new RuntimeException("Expected =, ; or , after let statement variable at " + lineNumber);
			}
		}
		if (hasAnyAuto && (re == null))
			throw new RuntimeException("Cannot infer types without assignment at " + lineNumber);
		return new RALLetStatement(lineNumber, names.toArray(new String[0]), types.toArray(new RALType[0]), re);
	}

	public static Object[] parseStringEmbed(InsideFileContext ifc, boolean expr) {
		Lexer lx = ifc.lexer;
		LinkedList<Object> obj = new LinkedList<>();
		while (true) {
			Token tkn2 = lx.requireNext();
			if (tkn2 instanceof StrEmb) {
				Token.StrEmb se = (Token.StrEmb) tkn2;
				if (se.startIsClusterEnd)
					throw new RuntimeException("Unexpected inline cluster end at " + tkn2.lineNumber);
				// While inside the string embedding...
				while (true) {
					obj.add(se.text);
					if (!se.endIsClusterStart)
						break;
					obj.add(ParserExpr.parseExpr(ifc, true));
					tkn2 = lx.requireNext();
					if (tkn2 instanceof StrEmb) {
						se = (StrEmb) tkn2;
						if (!se.startIsClusterEnd)
							throw new RuntimeException("Expected inline cluster end at " + tkn2.lineNumber);
					} else {
						throw new RuntimeException("Unexpectedly lost in inline cluster at " + tkn2.lineNumber);
					}
				}
			} else {
				// expression inline statements don't use this!
				if (!expr) {
					if (tkn2.isKeyword(";"))
						break;
					throw new RuntimeException("String embedding or semicolon expected at " + tkn2.lineNumber);
				} else {
					throw new RuntimeException("String embedding expected at " + tkn2.lineNumber);
				}
			}
			// expression inline statements only last for a single embedding
			if (expr)
				break;
		}
		return obj.toArray();
	}

	public static void stmtCompletionIntents(InsideFileContext ifc) {
		ParserExpr.exprCompletionIntents(ifc);
	}
}
