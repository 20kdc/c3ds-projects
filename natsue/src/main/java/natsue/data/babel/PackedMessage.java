/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.nio.ByteBuffer;

import natsue.data.IOUtils;

/**
 * Yes, these have to be decoded. If only to verify the sender.
 * This class covers two layers of framing to save some pain.
 * See "C2E Message" and "Packed Babel Message" formats.
 */
public class PackedMessage {
	// PBM header is 24 bytes, C2E message header is 12 
	public static final int HEADER_LEN = 36;
	public static final int HEADER_C2E_LEN = 12;
	public static final int TYPE_PRAY = 0;
	public static final int TYPE_WRIT = 1;

	public long senderUIN;
	public int messageType;
	public byte[] messageData;

	public PackedMessage(long uin, int t, byte[] data) {
		senderUIN = uin;
		messageType = t;
		messageData = data;
	}

	public PackedMessage(byte[] toDecode) {
		ByteBuffer b = PacketReader.wrapLE(toDecode);
		senderUIN = UINUtils.make(b.getInt(8), b.getInt(4) & 0xFFFF);
		// the removal of 12 bytes here is to account for the C2E message header
		int messageDataLen = b.getInt(12) - HEADER_C2E_LEN;
		if (messageDataLen > toDecode.length - HEADER_LEN)
			throw new IndexOutOfBoundsException("Not going to work");
		messageType = b.getInt(28);
		messageData = new byte[messageDataLen];
		System.arraycopy(toDecode, HEADER_LEN, messageData, 0, messageDataLen);
	}

	public byte[] toByteArray() {
		int len = messageData.length + HEADER_LEN;
		ByteBuffer bb = IOUtils.newBuffer(len);
		bb.putInt(len);
		// INTENTIONALLY REVERSED, see spec.
		bb.putInt(UINUtils.hid(senderUIN));
		bb.putInt(UINUtils.uid(senderUIN));
		bb.putInt(messageData.length + HEADER_C2E_LEN);
		bb.putInt(0);
		bb.putInt(1);
		// ---
		bb.putInt(0x0C);
		bb.putInt(messageType);
		bb.putInt(0);
		bb.put(messageData);
		return bb.array();
	}
}
