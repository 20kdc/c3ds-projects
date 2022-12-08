/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.caos;

/**
 * Utilities for writing CAOS.
 */
public class CAOSUtils {
	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(String pfx, int va) {
		String res = Integer.toString(va);
		if (res.length() == 1)
			return pfx + "0" + res;
		return pfx + res;
	}

	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(int va) {
		return vaToString("va", va);
	}
}
