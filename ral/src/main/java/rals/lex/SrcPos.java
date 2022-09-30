/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

/**
 * Position in source code.
 */
public class SrcPos {
	public final String fileName;
	public final int line;
	public SrcPos(String fn, int l) {
		fileName = fn;
		line = l;
	}

	@Override
	public String toString() {
		return fileName + ":" + line;
	}
}
