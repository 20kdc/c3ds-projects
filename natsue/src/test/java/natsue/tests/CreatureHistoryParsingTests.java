/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cdsp.common.data.IOUtils;
import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSFeedHistory;

public class CreatureHistoryParsingTests {

	@Test
	public void test() {
		// User Text test
		CreatureHistoryBlob chb = parse("210300000100000002000000a0a84e11010000000d000000cb00000000000000200000003030312d6461776e2d36776134722d617a3878372d636e7634762d756c67676b00010000001000000049b01700f4020500789246630500000000000000000000000200000041311c000000646f636b2d79797666652d76343232682d79626733352d72626c74630b0000003239303336373634382b311500000000000000010000003d000000446f6f6d656420746f20666f72657665722077616e646572207468652077617374656c616e6473206f662074686520696d706f737369626c652e2e2e0a00000000");
		assertEquals(1, chb.events.length);
		assertEquals("dock-yyvfe-v422h-ybg35-rbltc", chb.events[0].worldID);
		assertEquals("", chb.name);
		assertEquals("Doomed to forever wander the wastelands of the impossible...\n", chb.userText);
	}

	private CreatureHistoryBlob parse(String packet) {
		// User text packet
		BaseCTOS data = TestUtils.packetHex(packet);
		CTOSFeedHistory cfh = TestUtils.checkedCastPacket(CTOSFeedHistory.class, data);
		return new CreatureHistoryBlob(IOUtils.wrapLE(cfh.data), 256);
	}
}
