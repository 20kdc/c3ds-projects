/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cdsp.common.data.Monikers;

public class MonikerVerificationSafetyTests {
	@Test
	public void test() {
		assertEquals(true, Monikers.verifyMoniker("001-dawn-6wa4r-az8x7-cnv4v-ulggk"));
		assertEquals(true, Monikers.verifyMoniker("12345-dawn-6wa4r-az8x7-cnv4v-ulggk"));
		assertEquals(false, Monikers.verifyMoniker("../194141-b-c-d-e-f"));
		assertEquals(false, Monikers.verifyMonikerBase("..", 1));
	}
}
