/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.nio.ByteBuffer;

import cdsp.common.data.IOUtils;
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

	public static byte[] writeHandshakeResponse(int errorCode, long serverUIN, long clientUIN) {
		ByteBuffer packet = IOUtils.newBuffer(60);
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
		ByteBuffer packet = IOUtils.newBuffer(message.length + 32);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, 9);
		packet.putInt(BaseCTOS.BASE_FIELD_FDLEN, message.length);
		packet.position(32);
		packet.put(message);
		return packet.array();
	}

	public static byte[] writeUserLine(boolean online, byte[] userData) {
		ByteBuffer packet = IOUtils.newBuffer(userData.length + 32);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, online ? 0x0D : 0x0E);
		packet.putInt(BaseCTOS.BASE_FIELD_FDLEN, userData.length);
		packet.position(32);
		packet.put(userData);
		return packet.array();
	}

	public static byte[] writeVirtualConnect(long initiatorUIN, short vsn) {
		ByteBuffer packet = IOUtils.newBuffer(44);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, 0x1E);
		packet.putInt(BaseCTOS.BASE_FIELD_C, UINUtils.uid(initiatorUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_D, UINUtils.hid(initiatorUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_E, vsn & 0xFFFF);
		return packet.array();
	}

	public static byte[] writeVirtualConnectResponse(short clientVSN, short serverVSN) {
		ByteBuffer packet = IOUtils.newBuffer(36);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, 0x14);
		int vsns = ((clientVSN & 0xFFFF) << 16) | (serverVSN & 0xFFFF);
		packet.putInt(BaseCTOS.BASE_FIELD_E, vsns);
		packet.putInt(32, 0xE);
		return packet.array();
	}

	public static byte[] writeVirtualCircuitData(long senderUIN, short senderVSN, long receiverUIN, short receiverVSN, byte[] data) {
		ByteBuffer packet = IOUtils.newBuffer(44 + data.length);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, 0x1F);
		packet.putInt(BaseCTOS.BASE_FIELD_C, UINUtils.uid(senderUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_D, UINUtils.hid(senderUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_FDLEN, data.length);
		int vsns = ((receiverVSN & 0xFFFF) << 16) | (senderVSN & 0xFFFF);
		packet.putInt(BaseCTOS.BASE_FIELD_E, vsns);
		packet.putInt(32, UINUtils.uid(receiverUIN));
		packet.putInt(36, UINUtils.hid(receiverUIN));
		packet.putInt(40, 2);
		byte[] res = packet.array();
		System.arraycopy(data, 0, res, 44, data.length);
		return res;
	}

	public static byte[] writeVirtualCircuitClose(long targetUIN) {
		ByteBuffer packet = IOUtils.newBuffer(32);
		packet.putInt(BaseCTOS.BASE_FIELD_TYPE, 0x20);
		packet.putInt(BaseCTOS.BASE_FIELD_C, UINUtils.uid(targetUIN));
		packet.putInt(BaseCTOS.BASE_FIELD_D, UINUtils.hid(targetUIN));
		return packet.array();
	}

	/**
	 * This packet is just for stupid broken network hardware's sake.
	 */
	public static byte[] writeDummy() {
		return writeVirtualCircuitClose(0);
	}
}
