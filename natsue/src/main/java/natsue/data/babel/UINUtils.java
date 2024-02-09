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
	// Natsue uses HIDs as a namespacing mechanism.
	// This isn't necessarily authentic, but who cares?
	/**
	 * HID for regular users.
	 * NOTE: To reduce shenanigans, accesses to this are centralized here but with poking by WhoisBotCommand.
	 * This means that UINUtils is the central hub for all UID/HID conversions.
	 */
	public static final int HID_USER = 1;
	public static final int HID_SYSTEM = 2;
	// For the server, but also for the system user
	public static final long SERVER_UIN = UINUtils.make(1, UINUtils.HID_SYSTEM);
	// For Virtual Circuit research
	public static final long VC_RESEARCH_UIN = UINUtils.make(2, UINUtils.HID_SYSTEM);

	public static int uid(long uin) {
		return (int) ((uin >> 32) & 0xFFFFFFFFL);
	}
	public static int hid(long uin) {
		return (short) (uin & 0xFFFF);
	}

	public static boolean isRegularUser(long uin) {
		return hid(uin) == HID_USER;
	}
	public static long ofRegularUser(int uid) {
		return make(uid, HID_USER);
	}

	public static int asDBUID(long uin) {
		return isRegularUser(uin) ? uid(uin) : 0;
	}

	public static long make(int uid, int hid) {
		// the masking here is very important, it prevents issues with "uninitialized padding" HIDs
		long uidl = uid & 0xFFFFFFFFL;
		long hidl = hid & 0x0000FFFFL;
		return (uidl << 32) | hidl;
	}

	public static String toString(long targetUIN) {
		return uid(targetUIN) + "+" + hid(targetUIN);
	}

	/**
	 * Returns -1 on error (as -1 is an invalid UIN).
	 */
	public static long valueOf(String str) {
		int plusIdx = str.indexOf('+');
		if (plusIdx >= 0) {
			try {
				String a = str.substring(0, plusIdx);
				String b = str.substring(plusIdx + 1);
				return UINUtils.make(Integer.valueOf(a), Integer.valueOf(b));
			} catch (Exception ex) {
				return -1;
			}
		}
		return -1;
	}
}
