/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import natsue.data.babel.ctos.BaseCTOS;

/**
 * Functions to write out packets.
 */
public class PacketWriter {
	// not exhaustive but good enough
	public static int HANDSHAKE_RESPONSE_OK = 0;
	public static int HANDSHAKE_RESPONSE_ALREADY_LOGGED_IN = 1;
	public static int HANDSHAKE_RESPONSE_INTERNAL = 2;
	public static int HANDSHAKE_RESPONSE_INVALID_USER = 3;
	public static int HANDSHAKE_RESPONSE_TOO_MANY_USERS = 12;
	public static int HANDSHAKE_RESPONSE_NEEDS_UPDATE = 14;
	public static int HANDSHAKE_RESPONSE_UNKNOWN = 16;

	public static ByteBuffer newBuffer(int size) {
		ByteBuffer res = ByteBuffer.allocate(size);
		res.order(ByteOrder.LITTLE_ENDIAN);
		return res;
	}

	public static byte[] writeHandshakeResponse(int errorCode, long serverUIN, long clientUIN) {
		ByteBuffer packet = newBuffer(60);
		packet.put(BaseCTOS.BASE_FIELD_TYPE, (byte) 10);
		packet.put(1, (byte) errorCode);
		packet.putInt(BaseCTOS.BASE_FIELD_A, UINUtils.uid(serverUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_B, UINUtils.hid(serverUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_C, UINUtils.uid(clientUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_D, UINUtils.hid(clientUIN));
		packet.putInt(44, 12);
		return packet.array();
	}

	public static byte[] writeMessage(byte[] message) {
		ByteBuffer packet = newBuffer(message.length + 32);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, 9);
		packet.putInt(BaseCTOS.BASE_FIELD_FDLEN, message.length);
		packet.position(32);
		packet.put(message);
		return packet.array();
	}

	public static byte[] writeUserLine(boolean online, byte[] userData) {
		ByteBuffer packet = newBuffer(userData.length + 32);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, online ? 0x0D : 0x0E);
		packet.putInt(BaseCTOS.BASE_FIELD_FDLEN, userData.length);
		packet.position(32);
		packet.put(userData);
		return packet.array();
	}
}
