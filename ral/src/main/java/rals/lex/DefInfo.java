/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import rals.diag.SrcPos;
import rals.diag.SrcRange;

/**
 * Used to refer to a token when we don't want to make it too clear we're just passing tokens around blindly...
 */
public abstract class DefInfo {
	// nullable except for At
	public final SrcRange srcRange;
	// nullable generally
	public final String docComment;

	private DefInfo(SrcRange sr, String d) {
		srcRange = sr;
		docComment = d;
	}

	public static class Builtin extends DefInfo {
		public Builtin(String d) {
			super(null, d);
		}
	}

	public static class At extends DefInfo {
		public At(Token st, Token en) {
			super(st.extent.expand(en.extent), st.docComment);
		}

		public At(SrcPos lineNumber, String string) {
			super(new SrcRange(lineNumber, lineNumber), string);
		}
	}
}
