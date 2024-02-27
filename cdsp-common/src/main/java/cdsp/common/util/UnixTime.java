/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.util;

/**
 * Quick thing to stuff this into.
 */
public class UnixTime {
	/**
	 * Gets unix time in seconds since the epoch.
	 */
	public static long get() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * With 2038 coming up alarmingly fast, use a sliding window to infer time.
	 */
	public static long inferFrom32(int unixTime32, long sendUnixTime) {
		long divider = (sendUnixTime & 0xFFFFFFFFL) ^ 0x80000000L;
		long unsigned = unixTime32 & 0xFFFFFFFFL;
		long eraBase = sendUnixTime & ~0xFFFFFFFFL;
		// this works because of very heavy unit testing
		if ((divider & 0x80000000L) != 0) {
			if (divider > unsigned) {
				return unsigned + eraBase;
			} else {
				return unsigned + eraBase - 0x100000000L;
			}
		} else {
			if (divider > unsigned) {
				return unsigned + eraBase + 0x100000000L;
			} else {
				return unsigned + eraBase;
			}
		}
	}
}
