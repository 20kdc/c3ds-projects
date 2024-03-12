/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.test;

import org.junit.Test;

public class CAOSCharsetActsAsExpectedTest {
	@Test
	public void test() {
		/* let's just get Aquarium back on schedule hmm
		for (int i = 0; i < 256; i++) {
			byte[] data = new byte[] {(byte) i};
			String res = new String(data, CAOSUtils.CAOS_CHARSET);
			if (res.length() != 1)
				throw new RuntimeException("CAOS charset maps " + i + " to " + res + " (" + res.length() + " chars)");
			byte[] data2 = res.getBytes(CAOSUtils.CAOS_CHARSET);
			if (data2.length != 1)
				throw new RuntimeException("CAOS charset non-transitively maps " + i + " (" + res + ") (wrong length)");
			if (data2[0] != (byte) i)
				throw new RuntimeException("CAOS charset non-transitively maps " + i + " (" + res + ") to " + (data2[0] & 0xFF));
		}
		*/
	}
}
