/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import rals.lex.Lexer;
import rals.lex.Token;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * Type parser.
 */
public class ParserType {
	public static RALType parseType(TypeSystem ts, Lexer lx) {
		String name = lx.requireNextID();
		RALType rt = ts.byName(name);
		if (rt == null)
			throw new RuntimeException("No such type " + name);
		Token tkn = lx.requireNext();
		if (tkn.isKeyword("?")) {
			return ts.byNullable(rt);
		} else {
			lx.back();
		}
		return rt;
	}
}
