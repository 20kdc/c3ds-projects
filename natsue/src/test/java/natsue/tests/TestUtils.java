/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.tests;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import natsue.config.Config;
import natsue.data.babel.PacketReader;
import natsue.data.babel.ctos.BaseCTOS;

/**
 * Utilities for testing.
 */
public class TestUtils {
	public static BaseCTOS packetHex(String hex) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while (hex.length() > 0) {
				baos.write(Integer.parseUnsignedInt(hex.substring(0, 2), 16));
				hex = hex.substring(2);
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			byte[] header = new byte[PacketReader.PACKET_HEADER_SIZE];
			bais.read(header);
			Config defCfg = new Config();
			return PacketReader.readPacket(defCfg, header, bais);
		} catch (Exception ex) {
			throw new RuntimeException("During " + hex, ex);
		}
	}
	@SuppressWarnings("unchecked")
	public static <T extends BaseCTOS> T checkedCastPacket(Class<T> cls, BaseCTOS bc) {
		if (cls.isInstance(bc))
			return (T) bc;
		fail("Packet was expected to be " + cls + " not " + bc.getClass());
		return null;
	}
}
