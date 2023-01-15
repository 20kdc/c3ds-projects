/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.names;

import natsue.config.ConfigMessages;

/**
 * Verifies monikers are sane.
 * Moniker of maximum length:
 * 2147483647-aaaa-aaaaa-aaaaa-aaaaa-aaaaa
 * 123456789012345678901234567890123456789
 *          1         2         3
 */
public class CreatureDataVerifier {
	/**
	 * Maximum length of a moniker.
	 * Note that the database limit for this is 64 characters, to add some room for updates.
	 */
	private static final int MAX_MONIKER_LEN = 39;
	public static boolean verifyMoniker(String moniker) {
		if (moniker.length() < 32)
			return false;
		if (moniker.length() > MAX_MONIKER_LEN)
			return false;
		int componentCount = 1;
		for (char c : moniker.toCharArray()) {
			boolean ok = false;
			if (c >= 'a' && c <= 'z')
				ok = true;
			if (c >= '0' && c <= '9')
				ok = true;
			if (c == '-') {
				ok = true;
				componentCount++;
			}
			if (!ok)
				return false;
		}
		if (componentCount != 6)
			return false;
		return true;
	}
	public static boolean verifyWorldMoniker(String moniker) {
		if (moniker.length() != 28)
			return false;
		int componentCount = 1;
		for (char c : moniker.toCharArray()) {
			boolean ok = false;
			if (c >= 'a' && c <= 'z')
				ok = true;
			if (c >= '0' && c <= '9')
				ok = true;
			if (c == '-') {
				ok = true;
				componentCount++;
			}
			if (!ok)
				return false;
		}
		if (componentCount != 5)
			return false;
		return true;
	}

	private static String verifyLimitLen(int l, String name) {
		if (name.length() > l)
			name = name.substring(0, l);
		return name;
	}

	private static String verifyHumanReadableLen(int l, String name, boolean newLines) {
		return verifyLimitLen(l, name);
	}

	/**
	 * Verifies / strips a name.
	 */
	public static String stripMonikerLike(String name) {
		return verifyLimitLen(MAX_MONIKER_LEN, name);
	}

	/**
	 * Verifies / strips a name.
	 */
	public static String stripName(ConfigMessages config, String name) {
		return verifyHumanReadableLen(config.maxCreatureNameLen.getValue(), name, false);
	}

	/**
	 * Verifies / strips user text.
	 */
	public static String stripUserText(ConfigMessages config, String userText) {
		return verifyHumanReadableLen(config.maxCreatureUserTextLen.getValue(), userText, true);
	}
}
