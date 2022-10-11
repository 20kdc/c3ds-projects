/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.http;

import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Let's not have any XSS nonsense shall we?
 */
public class HTMLEncoder {
	public static final boolean[] safeASCII = new boolean[128];
	static {
		for (int i = 'a'; i <= 'z'; i++)
			safeASCII[i] = true;
		for (int i = 'A'; i <= 'Z'; i++)
			safeASCII[i] = true;
		for (int i = '0'; i <= '9'; i++)
			safeASCII[i] = true;
		safeASCII[' '] = true;
		safeASCII['-'] = true;
		safeASCII['+'] = true;
		safeASCII['('] = true;
		safeASCII[')'] = true;
		safeASCII['{'] = true;
		safeASCII['}'] = true;
		safeASCII['['] = true;
		safeASCII[']'] = true;
		safeASCII[','] = true;
		safeASCII['.'] = true;
		safeASCII['!'] = true;
	}
	public static void htmlEncode(StringBuilder sw, String inp) {
		for (char ch : inp.toCharArray()) {
			boolean safe = false;
			if (ch < 128)
				if (safeASCII[ch])
					safe = true;
			if (safe) {
				sw.append(ch);
			} else {
				sw.append("&#");
				sw.append((int) ch);
				sw.append(';');
			}
		}
	}

	public static String urlDecode(String substring) {
		try {
			return URLDecoder.decode(substring, "UTF-8");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String urlEncode(String nickname) {
		try {
			return URLEncoder.encode(nickname, "UTF-8");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String hrefEncode(String nickname) {
		return urlEncode(nickname);
	}
}
