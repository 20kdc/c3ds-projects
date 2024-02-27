/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.util;

/**
 * For database reasons. Not 100% accurate because it doesn't do the machine ID
 * thing. Instead the machine ID is used for upper timestamp bits but can also
 * be used for extra sequence numbers in a pickle.
 */
public class Snowflake {
	private static final Object syncObject = new Object();
	private static volatile long lastQuerySyncNumber = 0;
	private static volatile long sequenceNumber = 0;

	public static long generateSnowflake() {
		synchronized (syncObject) {
			long ctm = System.currentTimeMillis();
			if (lastQuerySyncNumber != ctm)
				sequenceNumber = 0;
			lastQuerySyncNumber = ctm;
			ctm -= 1288834974657L;
			ctm = (ctm << 22) | (((ctm >> 41) & 0x3FF) << 12);
			ctm |= sequenceNumber++;
			return ctm;
		}
	}
}
