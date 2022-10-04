/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

/**
 * This is similar to getInlineCAOS, but it's for very specific circumstances.
 * Basically, if the logic would *otherwise* involve doing string operations on getInlineCAOS, use this instead.
 */
public enum RALSpecialInline {
	None(null, false, true),
	Ownr("ownr", true, false),
	Targ("targ", false, false),
	VA(null, true, false),
	Discard(null, true, false);
	public final String code;
	public final boolean inlineWritable;
	public final boolean readCouldHaveSideEffects;
	RALSpecialInline(String s, boolean iw, boolean rse) {
		code = s;
		inlineWritable = iw;
		readCouldHaveSideEffects = rse;
	}
}