/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

/**
 * Natsue Server stores UINs as longs for convenience's sake.
 * This class deals with that. 
 */
public class UINUtils {
	public static int uid(long uin) {
		return (int) ((uin >> 32) & 0xFFFFFFFFL);
	}
	public static int hid(long uin) {
		return (short) (uin & 0xFFFF);
	}
	public static long make(int uid, int hid) {
		long uidl = uid & 0xFFFFFFFFL;
		long hidl = hid & 0x0000FFFFL;
		return (uidl << 32) | hidl;
	}
}
