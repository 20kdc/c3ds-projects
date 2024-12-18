/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

/**
 * Verifies monikers are sane.
 * Moniker of maximum length:
 * 2147483647-aaaa-aaaaa-aaaaa-aaaaa-aaaaa
 * 123456789012345678901234567890123456789
 *          1         2         3
 */
public class Monikers {
	/**
	 * Maximum length of a moniker.
	 * Note that the database limit for this is 64 characters, to add some room for updates.
	 */
	public static final int MAX_MONIKER_LEN = 39;
	public static final int MONIKER_CREATURE_COMPONENTS = 6;
	public static final int MONIKER_WORLD_COMPONENTS = 5;

	public static boolean verifyMoniker(String moniker) {
		if (moniker.length() < 32)
			return false;
		if (moniker.length() > MAX_MONIKER_LEN)
			return false;
		return verifyMonikerBase(moniker, MONIKER_CREATURE_COMPONENTS);
	}
	public static boolean verifyWorldMoniker(String moniker) {
		if (moniker.length() != 28)
			return false;
		return verifyMonikerBase(moniker, MONIKER_WORLD_COMPONENTS);
	}
	public static boolean verifyMonikerBase(String moniker, int targetComponents) {
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
		if (componentCount != targetComponents)
			return false;
		return true;
	}

}
