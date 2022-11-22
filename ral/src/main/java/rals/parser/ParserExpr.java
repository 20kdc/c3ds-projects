/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.HashSet;
import java.util.LinkedList;

import rals.cond.*;
import rals.diag.SrcPos;
import rals.expr.*;
import rals.expr.RALConstant.Int;
import rals.expr.RALConstant.Str;
import rals.lex.*;
import rals.stmt.*;
import rals.types.*;

/**
 * Expression parser.
 */
public class ParserExpr {
	public static final String[][] operatorPrecedenceGroups = new String[][] {
		// Outermost in tree to innermost
		{","},
		{"||"},
		{"&&"},
		{"==", "!=", "<=", ">=", "<", ">"},
		{"+", "-"},
		{"/", "*"},
		{"|", "&"}
	};
	public static final HashSet<String> allOps = new HashSet<>();
	static {
		for (String[] s : operatorPrecedenceGroups)
			for (String t : s)
				allOps.add(t);
	}

	public static RALConstant parseConst(InsideFileContext ifc) {
		RALExprUR ex = parseExpr(ifc, true);
		RALConstant ex2 = ex.resolveConst(ifc.typeSystem);
		if (ex2 == null)
			throw new RuntimeException("Unable to resolve " + ex + " to constant expression.");
		return ex2;
	}
	public static int parseConstInteger(InsideFileContext ifc) {
		RALConstant re = parseConst(ifc);
		if (re instanceof RALConstant.Int) {
			return ((RALConstant.Int) re).value;
		} else {
			throw new RuntimeException("Expected constant integer.");
		}
	}
	public static String parseConstString(InsideFileContext ifc) {
		RALConstant re = parseConst(ifc);
		if (re instanceof RALConstant.Str) {
			return ((RALConstant.Str) re).value;
		} else {
			throw new RuntimeException("Expected constant string.");
		}
	}

	public static RALExprUR parseExpr(InsideFileContext ifc, boolean must) {
		RALExprUR expr = parseExprOrNull(ifc);
		if (expr == null) {
			if (must)
				ifc.diags.error(ifc.lexer.genLN(), "expected expression");
			return RALExprGroupUR.of();
		}
		return expr;
	}

	public static RALExprUR parseExprOrNull(InsideFileContext ifc) {
		Lexer lx = ifc.lexer;
		RALExprUR firstAtom = parseExprFullAtomOrNull(ifc);
		if (firstAtom == null)
			return null;
		LinkedList<RALExprUR> atoms = new LinkedList<>();
		LinkedList<String> ops = new LinkedList<>();
		atoms.add(firstAtom);
		while (true) {
			Token tkn = lx.next();
			if (tkn == null) {
				// nopers
				break;
			} else if (!(tkn instanceof Token.Kw)) {
				// definitely not
				lx.back();
				break;
			}
			String opId = ((Token.Kw) tkn).text;
			if (!allOps.contains(opId)) {
				// still no
				lx.back();
				break;
			}
			ops.add(opId);
			firstAtom = parseExprFullAtomOrNull(ifc);
			if (firstAtom == null)
				throw new RuntimeException(opId + " without expression at " + tkn.lineNumber);
			atoms.add(firstAtom);
		}
		// now for op processing
		RALExprUR[] atomsArr = atoms.toArray(new RALExprUR[0]);
		String[] opsArr = ops.toArray(new String[0]);
		return binopProcessor(atomsArr, opsArr, 0, opsArr.length);
	}

	public static RALExprUR binopProcessor(RALExprUR[] atomArr, String[] opArr, int aBase, int opCount) {
		if (opCount == 0)
			return atomArr[aBase];
		for (String[] pCl : operatorPrecedenceGroups) {
			// Scanning must be done in reverse, or else the chains will be right-heavy.
			// This turns 1 - 1 - 1 - 1 into 1 - (1 - (1 - 1)), which is 0.
			// It is expected to be (((1 - 1) - 1) - 1), which is -2.
			// This is also what RALChainOp expects.
			boolean reverse = true;
			int start = reverse ? (aBase + opCount - 1) : (aBase);
			int limit = reverse ? (aBase - 1) : (aBase + opCount);
			int dir = reverse ? -1 : 1;
			for (int i = start; i != limit; i += dir) {
				for (String op : pCl) {
					if (op.equals(opArr[i])) {
						// if i == aBase then opCount needs to be 0 (LHS is just the atom directly left)
						RALExprUR l = binopProcessor(atomArr, opArr, aBase, i - aBase);
						// if i == aBase + opCount - 1 then opCount needs to be 0 (RHS is just the atom directly right)
						RALExprUR r = binopProcessor(atomArr, opArr, i + 1, aBase + opCount - (i + 1));
						return binopMaker(l, opArr[i], r);
					}
				}
			}
		}
		throw new RuntimeException("Operator not in any precedence groups: " + opArr[0]);
	}

	private static RALExprUR binopMaker(RALExprUR l, String string, RALExprUR r) {
		if (string.equals(",")) {
			return RALExprGroupUR.of(l, r);
		} else if (string.equals("==")) {
			return new RALCondSimple(l, RALCondSimple.Op.Equal, r);
		} else if (string.equals("!=")) {
			return new RALCondSimple(l, RALCondSimple.Op.NotEqual, r);
		} else if (string.equals(">")) {
			return new RALCondSimple(l, RALCondSimple.Op.GreaterThan, r);
		} else if (string.equals(">=")) {
			return new RALCondSimple(l, RALCondSimple.Op.GreaterEqual, r);
		} else if (string.equals("<")) {
			return new RALCondSimple(l, RALCondSimple.Op.LessThan, r);
		} else if (string.equals("<=")) {
			return new RALCondSimple(l, RALCondSimple.Op.LessEqual, r);
		} else if (string.equals("&&")) {
			return new RALCondLogOp(l, RALCondLogOp.Op.And, r);
		} else if (string.equals("||")) {
			return new RALCondLogOp(l, RALCondLogOp.Op.Or, r);
		} else if (string.equals("+")) {
			return RALChainOp.of(l, RALChainOp.ADD, r);
		} else if (string.equals("-")) {
			return RALChainOp.of(l, RALChainOp.SUB, r);
		} else if (string.equals("/")) {
			return RALChainOp.of(l, RALChainOp.DIV, r);
		} else if (string.equals("*")) {
			return RALChainOp.of(l, RALChainOp.MUL, r);
		} else if (string.equals("&")) {
			return RALChainOp.of(l, RALChainOp.AND, r);
		} else if (string.equals("|")) {
			return RALChainOp.of(l, RALChainOp.OR, r);
		}
		throw new RuntimeException("No handler for binop " + string);
	}

	public static RALExprUR parseExprFullAtomOrNull(InsideFileContext ifc) {
		RALExprUR firstAtom = parseExprAtomOrNull(ifc);
		if (firstAtom == null)
			return null;
		return parseExprSuffix(firstAtom, ifc);
	}

	private static RALExprUR parseExprAtomOrNull(InsideFileContext ifc) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;

		Token tkn = lx.requireNext();
		if (tkn instanceof Token.Int) {
			return new RALConstant.Int(ts, ((Token.Int) tkn).value);
		} else if (tkn instanceof Token.Str) {
			return new RALConstant.Str(ts, ((Token.Str) tkn).text);
		} else if (tkn instanceof Token.Flo) {
			return new RALConstant.Flo(ts, ((Token.Flo) tkn).value);
		} else if (tkn instanceof Token.ID) {
			return new RALAmbiguousID(ts, ((Token.ID) tkn).text);
		} else if (tkn instanceof Token.StrEmb) {
			// So before we accept this, this could actually be a termination.
			Token.StrEmb se = (Token.StrEmb) tkn;
			// Either way we go back a token due to passing off parsing to the inliner stuff.
			// String embed syntax was first tested on inline statements, you see.
			lx.back();
			if (se.startIsClusterEnd)
				return null;
			// Ok, it's not. Use the same string embed parser as inline expressions use to start with.
			Object[] objs = ParserCode.parseStringEmbed(ifc, true);
			// Then convert the strings to constants.
			RALExprUR[] total = new RALExprUR[objs.length];
			for (int i = 0; i < total.length; i++) {
				Object o = objs[i];
				if (o instanceof String) {
					total[i] = new RALConstant.Str(ts, (String) o);
				} else if (o instanceof RALExprUR) {
					total[i] = (RALExprUR) o;
				} else {
					throw new RuntimeException("String embed parser isn't supposed to output this: " + o);
				}
			}
			// Because we ran into the string embed in the first place, this will have at least one value.
			return new RALChainOp(RALChainOp.ADD_STR, total);
		} else if (tkn.isKeyword("++") || tkn.isKeyword("--")) {
			RALExprUR inner = parseExprAtomOrNull(ifc);
			if (inner == null)
				throw new RuntimeException("Looked like the setup for a pre-inc/dec, but was not :" + tkn);
			return makeIncDec(tkn.lineNumber, ts, inner, true, tkn.isKeyword("++"));
		} else if (tkn.isKeyword("&")) {
			return new RALInlineExpr(ParserCode.parseStringEmbed(ifc, true));
		} else if (tkn.isKeyword("{")) {
			// Oh, this gets weird...
			RALBlock stmt = new RALBlock(tkn.lineNumber, false);
			RALExprUR ret = RALExprGroupUR.of();
			while (true) {
				Token chk = lx.requireNext();
				if (chk.isKeyword("}")) {
					break;
				} else if (chk.isKeyword("return")) {
					ret = parseExpr(ifc, false);
					lx.requireNextKw(";");
					lx.requireNextKw("}");
					break;
				}
				lx.back();
				stmt.content.add(ParserCode.parseStatement(ifc));
			}
			return new RALStmtExpr(stmt, ret);
		} else if (tkn.isKeyword("(")) {
			RALExprUR interior = parseExpr(ifc, false);
			lx.requireNextKw(")");
			return interior;
		} else if (tkn.isKeyword("!")) {
			// logical NOT
			// so the reason this uses parseExprFullAtomOrNull?
			// "if !ownr.cubeOccupied {"
			// the field access is a suffix, so...
			RALExprUR interior = parseExprFullAtomOrNull(ifc);
			if (interior == null)
				throw new RuntimeException("Logical NOT with no expression at " + tkn.lineNumber);
			return new RALCondInvert(interior);
		} else if (tkn.isKeyword("~")) {
			// bitwise NOT
			RALExprUR interior = parseExprFullAtomOrNull(ifc);
			if (interior == null)
				throw new RuntimeException("Bitwise NOT with no expression at " + tkn.lineNumber);
			return new RALBitInvert(interior);
		} else {
			lx.back();
			return null;
		}
	}

	private static RALExprUR makeIncDec(SrcPos ln, TypeSystem ts, RALExprUR inner, boolean pre, boolean inc) {
		RALChainOp.Op op = inc ? RALChainOp.ADD : RALChainOp.SUB;
		RALStatementUR mod = new RALAssignStatement(ln, inner, RALChainOp.of(inner, op, new RALConstant.Int(ts, 1)));
		if (pre) {
			// return value is after the adjustment
			// so perform the operation on the initial value, then return what we got in the first place
			return new RALStmtExpr(mod, inner);
		} else {
			String idStr = ts.newParserVariableName();
			RALAmbiguousID id = new RALAmbiguousID(ts, idStr);
			// return value is before the adjustment
			// so store a temporary before the operation
			RALBlock blk = new RALBlock(ln, false);
			blk.content.add(new RALLetStatement(ln, new String[] {idStr}, new RALType[] {null}, inner));
			blk.content.add(mod);
			return new RALStmtExpr(blk, id);
		}
	}
	private static RALExprUR parseExprSuffix(RALExprUR base, InsideFileContext ifc) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;

		while (true) {
			Token tkn = lx.next();
			if (tkn == null) {
				// well, that ends that
				return base;
			} else if (tkn.isKeyword("instanceof")) {
				RALType rt = ParserType.parseType(ts, lx);
				if (!(rt instanceof RALType.AgentClassifier))
					throw new RuntimeException("instanceof requires class not " + rt);
				base = new RALInstanceof(((RALType.AgentClassifier) rt).classifier, base);
			} else if (tkn.isKeyword("++") || tkn.isKeyword("--")) {
				// post-inc/dec
				base = makeIncDec(tkn.lineNumber, ts, base, false, tkn.isKeyword("++"));
			} else if (tkn.isKeyword("(")) {
				// Call.
				RALExprUR group = ParserExpr.parseExpr(ifc, false);
				lx.requireNextKw(")");
				if (base instanceof RALAmbiguousID) {
					base = new RALCall(((RALAmbiguousID) base).text, group);
				} else {
					throw new RuntimeException("You can't put a call on anything but an ambiguous ID, and certainly not " + base);
				}
			} else if (tkn.isKeyword(":") || tkn.isKeyword("->")) {
				boolean isScript = tkn.isKeyword(":");
				String msgName = lx.requireNextID();
				// Determine if we might be intervening in an emit expression and cancel if so
				if (lx.optNextKw("(")) {
					// We *are* intervening in an emit expression, get out of here
					lx.back(); // (
					lx.back(); // ID
					lx.back(); // : / ->
					return base;
				}
				if (base instanceof RALAmbiguousID) {
					String typeName = ((RALAmbiguousID) base).text;
					RALType rt = ts.byName(typeName);
					Integer msgId = rt.lookupMSID(msgName, isScript);
					if (msgId == null) {
						String lTp = isScript ? "script" : "message";
						String lOp = isScript ? ":" : "->";
						throw new RuntimeException("No such " + lTp + " " + typeName + lOp + msgName);
					}
					base = new RALConstant.Int(ts, msgId);
				} else {
					throw new RuntimeException("You can't get the message ID of anything but an ambiguous ID, and certainly not " + base);
				}
			} else if (tkn.isKeyword(".")) {
				String fieldName = lx.requireNextID();
				base = new RALFieldAccess(base, fieldName);
			} else if (tkn.isKeyword("!")) {
				// Forced cast.
				// If followed immediately by an ID, it's a cast to a specific type.
				// Otherwise, it's a nullability cast.
				Token tkn2 = lx.requireNext();
				lx.back();
				if (tkn2 instanceof Token.ID) {
					base = RALCast.of(base, ParserType.parseType(ts, lx));
				} else {
					base = new RALCast.Denull(base);
				}
			} else {
				lx.back();
				break;
			}
		}
		return base;
	}
}
