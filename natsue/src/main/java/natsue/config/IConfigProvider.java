/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

/**
 * Just to simplify things.
 */
public interface IConfigProvider {
	default int getConfigInt(String name, int defaultVal) {
		String res = getConfigString(name, Integer.toString(defaultVal));
		try {
			return Integer.parseInt(res);
		} catch (Exception ex) {
			// nope
			return defaultVal;
		}
	}
	String getConfigString(String name, String defaultVal);
}
