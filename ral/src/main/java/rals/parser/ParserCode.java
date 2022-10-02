/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.LinkedList;

import rals.expr.*;
import rals.lex.*;
import rals.stmt.*;
import rals.types.*;

/**
 * Code parser.
 */
public class ParserCode {
	public static RALStatementUR parseStatement(TypeSystem ts, Lexer lx) {
		Token tkn = lx.requireNext();
		if (tkn.isKeyword("{")) {
			RALBlock rb = new RALBlock(tkn.lineNumber, true);
			while (true) {
				tkn = lx.requireNext();
				if (tkn.isKeyword("}"))
					break;
				lx.back();
				rb.content.add(parseStatement(ts, lx));
			}
			return rb;
		} else if (tkn.isKeyword("inline")) {
			LinkedList<Object> obj = new LinkedList<>();
			while (true) {
				Token tkn2 = lx.requireNext();
				if (tkn2.isKeyword(";")) {
					break;
				} else if (tkn2 instanceof Token.Str) {
					obj.add(((Token.Str) tkn2).text);
				} else {
					lx.back();
					obj.add(ParserExpr.parseExpr(ts, lx, true));
				}
			}
			return new RALInlineStatement(tkn.lineNumber, obj.toArray());
		} else if (tkn.isKeyword("let")) {
			LinkedList<String> names = new LinkedList<>();
			LinkedList<RALType> types = new LinkedList<>();
			RALExprUR re = null;
			while (true) {
				RALType rt = ParserType.parseType(ts, lx);
				String n = lx.requireNextID();
				names.add(n);
				types.add(rt);
				Token chk = lx.requireNext();
				if (chk.isKeyword("=")) {
					re = ParserExpr.parseExpr(ts, lx, true);
					lx.requireNextKw(";");
					break;
				} else if (chk.isKeyword(";")) {
					break;
				} else if (!chk.isKeyword(",")) {
					throw new RuntimeException("Expected = or ,");
				}
			}
			return new RALLetStatement(tkn.lineNumber, names.toArray(new String[0]), types.toArray(new RALType[0]), re);
		} else if (tkn.isKeyword("alias")) {
			String id = lx.requireNextID();
			lx.requireNextKw("=");
			RALExprUR res = ParserExpr.parseExpr(ts, lx, true);
			lx.requireNextKw(";");
			return new RALAliasStatement(tkn.lineNumber, id, res);
		} else if (tkn.isKeyword("if")) {
			RALExprUR cond = ParserExpr.parseExpr(ts, lx, true);
			RALStatementUR body = ParserCode.parseStatement(ts, lx);
			RALStatementUR elseBranch = null;
			Token chk = lx.requireNext();
			if (chk.isKeyword("else")) {
				elseBranch = ParserCode.parseStatement(ts, lx);
			} else {
				lx.back();
			}
			return new RALIfStatement(tkn.lineNumber, cond, body, elseBranch, false);
		} else if (tkn.isKeyword("while")) {
			RALExprUR cond = ParserExpr.parseExpr(ts, lx, true);
			RALStatementUR body = ParserCode.parseStatement(ts, lx);
			RALBlock outerBlock = new RALBlock(tkn.lineNumber, true);
			outerBlock.content.add(new RALIfStatement(tkn.lineNumber, cond, new RALBreakFromLoop(tkn.lineNumber), null, true));
			outerBlock.content.add(body);
			return new RALBreakableLoop(tkn.lineNumber, outerBlock);
		} else if (tkn.isKeyword("break")) {
			lx.requireNextKw(";");
			return new RALBreakFromLoop(tkn.lineNumber);
		} else if (tkn.isKeyword("foreach")) {
			lx.requireNextKw("(");
			RALType iterOver = ParserType.parseType(ts, lx);
			lx.requireNextKw("in");
			String subType = lx.requireNextID();
			RALExprUR econAgent = null;
			if (subType.equals("econ")) {
				econAgent = ParserExpr.parseExpr(ts, lx, true);
			} else if (subType.equals("enum")) {
			} else if (subType.equals("epas")) {
			} else if (subType.equals("esee")) {
			} else if (subType.equals("etch")) {
			} else {
				throw new RuntimeException("Unrecognized subtype");
			}
			lx.requireNextKw(")");
			RALStatementUR body = ParserCode.parseStatement(ts, lx);
			return new RALEnumLoop(tkn.lineNumber, iterOver, subType, econAgent, body);
		} else {
			lx.back();
			RALExprUR target = ParserExpr.parseExpr(ts, lx, false);
			Token sp = lx.requireNext();
			if (sp.isKeyword(";")) {
				return new RALAssignStatement(tkn.lineNumber, null, target);
			} else if (sp.isKeyword("=")) {
				RALExprUR source = ParserExpr.parseExpr(ts, lx, true);
				return new RALAssignStatement(tkn.lineNumber, target.decomposite(), source);
			} else if (sp.isKeyword("->")) {
				// MESG WRT+
				Token messageId = lx.requireNext();
				RALExprUR getMsgType;
				if (messageId instanceof Token.ID) {
					getMsgType = new RALMessageIDGrabber(target, ((Token.ID) messageId).text);
				} else {
					lx.back();
					getMsgType = ParserExpr.parseExpr(ts, lx, true);
				}
				lx.requireNextKw("(");
				RALExprUR[] params = ParserExpr.parseExpr(ts, lx, false).decomposite();
				// work this out
				RALExprUR p1;
				RALExprUR p2;
				if (params.length == 0) {
					p1 = new RALConstant.Int(ts, 0);
					p2 = new RALConstant.Int(ts, 0);
				} else if (params.length == 1) {
					p1 = params[0];
					p2 = new RALConstant.Int(ts, 0);
				} else if (params.length == 2) {
					p1 = params[0];
					p2 = params[1];
				} else {
					throw new RuntimeException("Abnormal amount of parameters to emit statement");
				}
				lx.requireNextKw(")");
				RALExprUR after;
				if (lx.optNextKw("after")) {
					after = ParserExpr.parseExpr(ts, lx, true);
				} else {
					after = new RALConstant.Int(ts, 0);
				}
				lx.requireNextKw(";");
				return new RALInlineStatement(tkn.lineNumber, new Object[] {
					"mesg wrt+ ",
					RALCast.of(target, ts.gAgent, true),
					" ",
					RALCast.of(getMsgType, ts.gInteger, true),
					" ",
					RALCast.of(p1, ts.gAny, true),
					" ",
					RALCast.of(p2, ts.gAny, true),
					" ",
					RALCast.of(after, ts.gInteger, true),
					"\n"
				});
			} else {
				throw new RuntimeException("Saw expression at " + tkn + " but then was wrong about it.");
			}
		}
	}
}
