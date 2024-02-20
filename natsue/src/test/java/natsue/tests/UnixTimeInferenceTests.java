/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import natsue.server.database.UnixTime;

public class UnixTimeInferenceTests {
	@Test
	public void test() {
		// Confirm dates in the range 1970-4011 are reasonably valid
		// Importantly this checks cases where send time is not actually event time by some factor
		for (long i = 0; i != 0xF00000000L; i += 0x1000000) {
			for (int j = 0; j != 0xFFFF0000; j += 0x10000) {
				long trueTime = i + j;
				int reducedTime = (int) trueTime;
				assertEquals(trueTime, UnixTime.inferFrom32(reducedTime, i));
			}
		}
		for (int i = 0; i != 0x80000000; i += 0x1000000) {
			for (int j = -0x800; j != 0x800; j += 0x100) {
				assertEquals(i, UnixTime.inferFrom32(i, i));
			}
		}
	}
}
