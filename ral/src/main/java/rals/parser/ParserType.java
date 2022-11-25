/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.LinkedList;

import rals.hcm.HCMIntents;
import rals.lex.Lexer;
import rals.lex.Token;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * Type parser.
 */
public class ParserType {
	public static RALType parseType(InsideFileContext ifc) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;
		LinkedList<RALType> rts = new LinkedList<>();
		rts.add(parseTypeBranch(ifc));
		while (true) {
			Token tkn = lx.requireNext();
			if (!tkn.isKeyword("|")) {
				lx.back();
				break;
			}
			rts.add(parseTypeBranch(ifc));
		}
		return ts.byUnion(rts);
	}
	public static RALType parseTypeBranch(InsideFileContext ifc) {
		TypeSystem ts = ifc.typeSystem;
		Lexer lx = ifc.lexer;
		String name = parseTypeName(ifc);
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

	/**
	 * Parses a type name.
	 */
	public static String parseTypeName(InsideFileContext ifc) {
		ifc.hcm.addCompletionIntentToNextToken(HCMIntents.TYPE, true);
		Token.ID id = ifc.lexer.requireNextIDTkn();
		return id.text;
	}
}
