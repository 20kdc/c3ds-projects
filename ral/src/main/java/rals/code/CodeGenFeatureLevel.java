/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

/**
 * Controls engine features used by output code.
 */
public enum CodeGenFeatureLevel {
	c3(false, true, "Creatures 3"),
	ds(true, true, "Docking Station"),
	customEngine(true, false, "Custom Engine");

	public final boolean hasMVXX, requiresEnumBreakout;
	public final String defInfo;

	CodeGenFeatureLevel(boolean mv, boolean breakout, String details) {
		hasMVXX = mv;
		requiresEnumBreakout = breakout;
		defInfo = details;
	}
}
