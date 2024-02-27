/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import cdsp.common.util.TOTP;
import cdsp.common.util.TOTP.InvalidTOTPKeyException;

public class TOTPTests {

	@Test
	public void test() throws InvalidTOTPKeyException {
		// HOTP truncation tests
		assertEquals(872921, TOTP.hashToDigits((byte) 0x1F, (byte) 0x86, (byte) 0x98, (byte) 0x69, (byte) 0x0E, (byte) 0x02, (byte) 0xCA, (byte) 0x16, (byte) 0x61, (byte) 0x85, (byte) 0x50, (byte) 0xEF, (byte) 0x7F, (byte) 0x19, (byte) 0xDA, (byte) 0x8E, (byte) 0x94, (byte) 0x5B, (byte) 0x55, (byte) 0x5A));
		// NOT A REAL TOTP KEY
		// oathtool --hotp -b NTCRADLE -v -c 56945790
		// (yes these are identical)
		byte[] key = TOTP.decodeBase32("NTCRADLE");
		assertEquals("NTCRADLE", new String(TOTP.encodeBase32(key)));
		assertEquals(329666, TOTP.calculate(key, 56945790));
	}

}
