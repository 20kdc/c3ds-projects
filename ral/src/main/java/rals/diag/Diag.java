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
	public final SrcPos location;
	public final String text;
	public final String shortText;

	public Diag(Kind k, SrcPos loc, String t, String txs) {
		kind = k;
		location = loc;
		text = t;
		shortText = txs;
	}

	public enum Kind {
		// Error
		Error
	}
}
