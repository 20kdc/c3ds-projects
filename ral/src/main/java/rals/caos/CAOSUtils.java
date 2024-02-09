/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.caos;

import java.nio.charset.Charset;

/**
 * Utilities for writing CAOS.
 */
public class CAOSUtils {
	/**
	 * Character set for a standard copy of Creatures 3 or Docking Station.
	 */
	public static final Charset CAOS_CHARSET = Charset.forName("Cp1252");

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
		// [CAOS]
		return vaToString("va", va);
	}

	/**
	 * Unescapes the result of the OUTX command.
	 */
	public static String unescapeOUTX(String string) {
		StringBuilder sb = new StringBuilder();
		if (!string.startsWith("\""))
			throw new RuntimeException("unescapeOUTX: " + string);
		if (!string.endsWith("\""))
			throw new RuntimeException("unescapeOUTX: " + string);
		string = string.substring(1, string.length() - 1);
		boolean isEscaping = false;
		for (char c : string.toCharArray()) {
			if (isEscaping) {
				char conv = c;
				if (c == 'n') {
					conv = '\n';
				} else if (c == 'r') {
					conv = '\r';
				} else if (c == '0') {
					conv = 0;
				} else if (c == 't') {
					conv = '\t';
				}
				sb.append(conv);
				isEscaping = false;
			} else if (c == '\\') {
				isEscaping = true;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
