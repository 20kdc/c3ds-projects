/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.HashSet;
import java.util.LinkedList;

import rals.cond.RALCondLogOp;
import rals.cond.RALCondSimple;
import rals.expr.RALAmbiguousID;
import rals.expr.RALCall;
import rals.expr.RALCast;
import rals.expr.RALConstant;
import rals.expr.RALDiscard;
import rals.expr.RALExpr;
import rals.expr.RALExprGroup;
import rals.expr.RALExprUR;
import rals.expr.RALStmtExpr;
import rals.lex.Lexer;
import rals.lex.Token;
import rals.stmt.RALBlock;
import rals.stmt.RALStatementUR;
import rals.types.RALType;
import rals.types.TypeSystem;

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
		{"/", "*"}
	};
	public static final HashSet<String> allOps = new HashSet<>();
	static {
		for (String[] s : operatorPrecedenceGroups)
			for (String t : s)
				allOps.add(t);
	}

	public static RALConstant parseConst(TypeSystem ts, Lexer lx) {
		RALExprUR ex = parseExpr(ts, lx, true);
		RALExpr ex2 = ex.resolve(null);
		if (!(ex2 instanceof RALConstant))
			throw new RuntimeException("Unable to resolve " + ex + " to constant expression.");
		return (RALConstant) ex2;
	}
	public static int parseConstInteger(TypeSystem ts, Lexer lx) {
		RALConstant re = parseConst(ts, lx);
		if (re instanceof RALConstant.Int) {
			return ((RALConstant.Int) re).value;
		} else {
			throw new RuntimeException("Expected constant integer.");
		}
	}
	public static String parseConstString(TypeSystem ts, Lexer lx) {
		RALConstant re = parseConst(ts, lx);
		if (re instanceof RALConstant.Str) {
			return ((RALConstant.Str) re).value;
		} else {
			throw new RuntimeException("Expected constant string.");
		}
	}

	public static RALExprUR parseExpr(TypeSystem ts, Lexer lx, boolean must) {
		RALExprUR firstAtom = parseExprFullAtomOrNull(ts, lx);
		if (firstAtom == null) {
			if (must)
				throw new RuntimeException("expected at least one expression around " + lx.genLN());
			return RALExprGroup.of();
		}
		LinkedList<RALExprUR> atoms = new LinkedList<>();
		LinkedList<String> ops = new LinkedList<>();
		atoms.add(firstAtom);
		while (true) {
			Token tkn = lx.requireNext();
			if (!(tkn instanceof Token.Kw)) {
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
			firstAtom = parseExprFullAtomOrNull(ts, lx);
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
			for (int i = aBase; i < aBase + opCount; i++) {
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
			return RALExprGroup.of(l, r);
		} else if (string.equals("==")) {
			return new RALCondSimple(l, "eq", r);
		} else if (string.equals("!=")) {
			return new RALCondSimple(l, "ne", r);
		} else if (string.equals(">")) {
			return new RALCondSimple(l, "gt", r);
		} else if (string.equals(">=")) {
			return new RALCondSimple(l, "ge", r);
		} else if (string.equals("<")) {
			return new RALCondSimple(l, "lt", r);
		} else if (string.equals("<=")) {
			return new RALCondSimple(l, "le", r);
		} else if (string.equals("&&")) {
			return new RALCondLogOp(l, "and", r);
		} else if (string.equals("||")) {
			return new RALCondLogOp(l, "or", r);
		}
		throw new RuntimeException("No handler for binop " + string);
	}

	public static RALExprUR parseExprFullAtomOrNull(TypeSystem ts, Lexer lx) {
		RALExprUR firstAtom = parseExprAtomOrNull(ts, lx);
		if (firstAtom == null)
			return null;
		return parseExprSuffix(firstAtom, ts, lx);
	}

	private static RALExprUR parseExprAtomOrNull(TypeSystem ts, Lexer lx) {
		Token tkn = lx.requireNext();
		if (tkn instanceof Token.Int) {
			return new RALConstant.Int(ts, ((Token.Int) tkn).value);
		} else if (tkn instanceof Token.Str) {
			return new RALConstant.Str(ts, ((Token.Str) tkn).text);
		} else if (tkn instanceof Token.Flo) {
			return new RALConstant.Flo(ts, ((Token.Flo) tkn).value);
		} else if (tkn instanceof Token.ID) {
			return new RALAmbiguousID(ts, ((Token.ID) tkn).text);
		} else if (tkn.isKeyword("{")) {
			// Oh, this gets weird...
			RALBlock stmt = new RALBlock(tkn.lineNumber, false);
			RALExprUR ret = RALExprGroup.of();
			while (true) {
				Token chk = lx.requireNext();
				if (chk.isKeyword("}")) {
					break;
				} else if (chk.isKeyword("return")) {
					ret = parseExpr(ts, lx, false);
					lx.requireNextKw(";");
					lx.requireNextKw("}");
					break;
				}
				lx.back();
				stmt.content.add(ParserCode.parseStatement(ts, lx));
			}
			return new RALStmtExpr(stmt, ret);
		} else if (tkn.isKeyword("(")) {
			RALExprUR interior = parseExpr(ts, lx, false);
			lx.requireNextKw(")");
			return interior;
		} else {
			lx.back();
			return null;
		}
	}

	private static RALExprUR parseExprSuffix(RALExprUR base, TypeSystem ts, Lexer lx) {
		while (true) {
			Token tkn = lx.requireNext();
			if (tkn.isKeyword("(")) {
				// Call.
				RALExprUR group = ParserExpr.parseExpr(ts, lx, false);
				lx.requireNextKw(")");
				if (base instanceof RALAmbiguousID) {
					base = new RALCall(((RALAmbiguousID) base).text, group);
				} else {
					throw new RuntimeException("You can't put a call on anything but an ambiguous ID, and certainly not " + base);
				}
			} else if (tkn.isKeyword(":")) {
				String msgName = lx.requireNextID();
				if (base instanceof RALAmbiguousID) {
					String typeName = ((RALAmbiguousID) base).text;
					RALType rt = ts.byName(typeName);
					Integer msgId = rt.lookupMSID(msgName, false);
					if (msgId == null)
						throw new RuntimeException("No such message " + typeName + ":" + msgName);
					return new RALConstant.Int(ts, msgId);
				} else {
					throw new RuntimeException("You can't get the message ID of anything but an ambiguous ID, and certainly not " + base);
				}
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
