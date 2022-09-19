/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import natsue.config.Config;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSClientCommand;
import natsue.data.babel.ctos.CTOSFeedHistory;
import natsue.data.babel.ctos.CTOSFetchRandomUser;
import natsue.data.babel.ctos.CTOSGetClientInfo;
import natsue.data.babel.ctos.CTOSGetConnectionDetail;
import natsue.data.babel.ctos.CTOSHandshake;
import natsue.data.babel.ctos.CTOSMessage;
import natsue.data.babel.ctos.CTOSUnknown;
import natsue.data.babel.ctos.CTOSWWRModify;

/**
 * General reference on reading packets.
 */
public class PacketReader {
	public static final int DEFAULT_MAXIMUM_BABEL_BINARY_MESSAGE_SIZE = 0x1000000;

	/**
	 * The standard character set used by the game.
	 */
	public static final Charset CHARSET = StandardCharsets.ISO_8859_1;

	/**
	 * Returns null for graceful start-of-read EOF, if allowed.
	 */
	public static byte[] getBytes(InputStream socketInput, int len, boolean canEOF) throws IOException {
		byte[] data = new byte[len];
		int ofs = 0;
		if (canEOF) {
			// If we're allowed to gracefully EOF here, then check for that with a special first read.
			ofs = socketInput.read(data, 0, len);
			// Check for graceful EOF.
			if (ofs <= 0)
				return null;
		}
		while (ofs < len) {
			int amount = socketInput.read(data, ofs, len - ofs);
			if (amount <= 0)
				throw new EOFException("Out of data");
			ofs += amount;
		}
		return data;
	}

	/**
	 * Returns null for graceful start-of-read EOF, if allowed.
	 */
	public static ByteBuffer getWrappedBytes(InputStream socketInput, int len, boolean canEOF) throws IOException {
		byte[] data = getBytes(socketInput, len, canEOF);
		if (data == null)
			return null;
		return wrapLE(data);
	}

	public static ByteBuffer wrapLE(byte[] total) {
		return wrapLE(total, 0, total.length);
	}

	public static ByteBuffer wrapLE(byte[] total, int ofs, int len) {
		ByteBuffer bb = ByteBuffer.wrap(total, ofs, len);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}

	public static long getUIN(ByteBuffer initial, int ofs) {
		// HID gets masked in this, which is good and important
		return UINUtils.make(initial.getInt(ofs), initial.getInt(ofs + 4));
	}

	/**
	 * Gets a string with "stream-like" ByteBuffer access
	 */
	public static String getString(ByteBuffer bb) {
		int len = bb.getInt();
		byte[] baseArray = bb.array();
		int pos = bb.position();
		String str = new String(baseArray, bb.arrayOffset() + pos, len, PacketReader.CHARSET);
		bb.position(pos + len);
		return str;
	}

	/**
	 * Reads the next packet from an input stream.
	 * Returns null if the connection ended gracefully.
	 */
	public static BaseCTOS readPacket(Config cfg, InputStream packetSource) throws IOException {
		ByteBuffer initial = getWrappedBytes(packetSource, 0x20, true);
		if (initial == null)
			return null;
		// alright, what type is this?
		int type = initial.getInt(BaseCTOS.BASE_FIELD_TYPE);
		BaseCTOS packetBase = packetInstanceByType(type);
		packetBase.initializeAndReadRemainder(cfg, packetSource, initial);
		return packetBase;
	}

	private static BaseCTOS packetInstanceByType(int type) {
		switch (type) {
		case 0x09:
			return new CTOSMessage();
		case 0x0F:
			return new CTOSGetClientInfo();
		case 0x10:
			return new CTOSWWRModify(true);
		case 0x11:
			return new CTOSWWRModify(false);
		case 0x12:
			// C_TID_NOTIFY_LISTENING_PORT
			return new CTOSUnknown(0, 0, false);
		case 0x13:
			return new CTOSGetConnectionDetail();
		case 0x14:
			return new CTOSClientCommand();
		case 0x18:
			// C_TID_GET_STATUS
			return new CTOSUnknown(0, 48, false);
		case 0x1E:
			// C_TID_VIRTUAL_CONNECT
			return new CTOSUnknown(12, 0, false);
		case 0x1F:
			// C_TID_VIRTUAL_CIRCUIT
			return new CTOSUnknown(12, 0, true);
		case 0x0221:
			return new CTOSFetchRandomUser();
		case 0x0321:
			return new CTOSFeedHistory();
		case 0x25:
			return new CTOSHandshake();
		}
		return new CTOSUnknown(0, 0, false);
	}
}
