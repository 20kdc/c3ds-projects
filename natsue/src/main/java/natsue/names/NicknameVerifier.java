/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.names;

/**
 * Verifies usernames are sane.
 */
public class NicknameVerifier {
	public static String foldNickname(String username) {
		return username.toLowerCase();
	}
	public static boolean verifyNickname(String username) {
		if (username.length() > 16)
			return false;
		for (char c : username.toCharArray()) {
			boolean charOk = false;
			if ((c >= 'a') && (c <= 'z')) {
				charOk = true;
			} else if ((c >= '0') && (c <= '9')) {
				charOk = true;
			}
			if (!charOk)
				return false;
		}
		return true;
	}
}
