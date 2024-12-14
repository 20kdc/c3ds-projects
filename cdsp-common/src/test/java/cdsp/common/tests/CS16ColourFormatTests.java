/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import cdsp.common.s16.CS16ColourFormat;

public class CS16ColourFormatTests {
	@Test
	public void test() {
		// RRRRRGGGGGGBBBBB RRRRRGGGGGBBBBB
		assertEquals((short) 0b1111100000000000, CS16ColourFormat.RGB555.to565((short) 0b111110000000000));
		assertEquals((short) 0b0000011111100000, CS16ColourFormat.RGB555.to565((short) 0b000001111100000));
		assertEquals((short) 0b0000000000011111, CS16ColourFormat.RGB555.to565((short) 0b000000000011111));
		// RRRRRGGGGGGBBBBB RRRRRGGGGGBBBBB
		assertEquals((short) 0b1011100000000000, CS16ColourFormat.RGB555.to565((short) 0b101110000000000));
		assertEquals((short) 0b0000010111100000, CS16ColourFormat.RGB555.to565((short) 0b000001011100000));
		assertEquals((short) 0b0000000000010111, CS16ColourFormat.RGB555.to565((short) 0b000000000010111));
		// ~~~
		// RRRRRGGGGGGBBBBB
		assertEquals(0x00FF0000, CS16ColourFormat.argbFrom565((short) 0b1111100000000000, false));
		assertEquals(0x0000FF00, CS16ColourFormat.argbFrom565((short) 0b0000011111100000, false));
		assertEquals(0x000000FF, CS16ColourFormat.argbFrom565((short) 0b0000000000011111, false));
		// 10111110 : BE
		// RRRRRGGGGGGBBBBB
		assertEquals(0x00BD0000, CS16ColourFormat.argbFrom565((short) 0b1011100000000000, false));
		assertEquals(0x0000BE00, CS16ColourFormat.argbFrom565((short) 0b0000010111100000, false));
		assertEquals(0x000000BD, CS16ColourFormat.argbFrom565((short) 0b0000000000010111, false));
	}
}
