/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

/**
 * Base "common" SrcPos stuff between LSP world and RAL world - just the line/character
 */
public class SrcPosBase {
	public final int line, character;

	/**
	 * This represents the line/character index in long form.
	 */
	public final long lcLong;

	public SrcPosBase(int l, int c) {
		line = l;
		character = c;
		lcLong = toLCLong(l, c);
	}

	/**
	 * Translates to a long.
	 * This is in LSP-friendly form.
	 */
	public static long toLCLong(int l, int c) {
		return (((long) l) << 32L) | (c & 0xFFFFFFFFL);
	}
}
