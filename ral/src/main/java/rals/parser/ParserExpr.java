/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.LinkedList;

import rals.expr.RALAmbiguousID;
import rals.expr.RALConstant;
import rals.expr.RALDiscard;
import rals.expr.RALExpr;
import rals.expr.RALExprGroup;
import rals.expr.RALExprUR;
import rals.lex.Lexer;
import rals.lex.Token;
import rals.types.TypeSystem;

/**
 * Expression parser.
 */
public class ParserExpr {
	public static RALConstant parseConst(TypeSystem ts, Lexer lx) {
		RALExpr re = parseExpr(ts, lx).resolve(null);
		if (!(re instanceof RALConstant))
			throw new RuntimeException("Unable to resolve " + re + " to constant expression.");
		return (RALConstant) re;
	}
	public static int parseConstInteger(TypeSystem ts, Lexer lx) {
		RALConstant re = parseConst(ts, lx);
		if (re instanceof RALConstant.Int) {
			return ((RALConstant.Int) re).value;
		} else {
			throw new RuntimeException("Expected constant integer.");
		}
	}

	public static RALExprUR parseExpr(TypeSystem ts, Lexer lx) {
		RALExprUR firstAtom = parseExprAtomOrNull(ts, lx);
		if (firstAtom == null)
			return RALDiscard.INSTANCE;
		firstAtom = parseExprSuffix(firstAtom, ts, lx);
		LinkedList<RALExprUR> atoms = new LinkedList<>();
		atoms.add(firstAtom);
		while (true) {
			Token tkn = lx.requireNext();
			if (!tkn.isKeyword(",")) {
				lx.back();
				break;
			}
			firstAtom = parseExprAtomOrNull(ts, lx);
			firstAtom = parseExprSuffix(firstAtom, ts, lx);
			atoms.add(firstAtom);
		}
		if (atoms.size() == 1) {
			return atoms.getFirst();
		} else {
			return RALExprGroup.of(atoms.toArray(new RALExprUR[0]));
		}
	}

	public static RALExprUR parseExprAtomOrNull(TypeSystem ts, Lexer lx) {
		Token tkn = lx.requireNext();
		if (tkn instanceof Token.Int) {
			return new RALConstant.Int(ts, ((Token.Int) tkn).value);
		} else if (tkn instanceof Token.Str) {
			return new RALConstant.Str(ts, ((Token.Str) tkn).text);
		} else if (tkn instanceof Token.Flo) {
			return new RALConstant.Flo(ts, ((Token.Flo) tkn).value);
		} else if (tkn instanceof Token.ID) {
			return new RALAmbiguousID(ts, ((Token.ID) tkn).text);
		} else {
			lx.back();
			return null;
		}
	}

	public static RALExprUR parseExprSuffix(RALExprUR base, TypeSystem ts, Lexer lx) {
		return base;
	}
}
