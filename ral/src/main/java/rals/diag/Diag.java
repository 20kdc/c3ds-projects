/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

/**
 * Something went wrong?
 */
public class Diag {
	public final Kind kind;
	/**
	 * Frames. Last is innermost frame.
	 */
	public final SrcRange[] frames;
	public final String text;
	public final String shortText;

	public Diag(Kind k, SrcRange[] f, String t, String txs) {
		if (f.length == 0)
			throw new RuntimeException("Diag with no stack frames: " + t);
		kind = k;
		frames = f;
		text = t;
		shortText = txs;
	}

	public enum Kind {
		Error,
		Warning;
	}
}
