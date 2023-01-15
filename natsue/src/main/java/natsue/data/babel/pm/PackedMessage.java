/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel.pm;

import java.nio.ByteBuffer;

import natsue.config.ConfigMessages;
import natsue.data.IOUtils;
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;
import natsue.data.babel.WritVal;
import natsue.data.pray.PRAYBlock;

/**
 * Yes, these have to be decoded. If only to verify the sender.
 * This class covers two layers of framing to save some pain.
 * See "C2E Message" and "Packed Babel Message" formats.
 */
public abstract class PackedMessage {
	// PBM header is 24 bytes, C2E message header is 12 
	public static final int HEADER_LEN = 36;
	public static final int HEADER_C2E_LEN = 12;
	public static final int TYPE_PRAY = 0;
	public static final int TYPE_WRIT = 1;

	public final int messageType;
	public long senderUIN;

	public PackedMessage(int type) {
		messageType = type;
	}

	public PackedMessage(long uin, int t) {
		senderUIN = uin;
		messageType = t;
	}

	public static PackedMessage read(byte[] toDecode, int prayMaxDecompressedSize) {
		ByteBuffer b = PacketReader.wrapLE(toDecode);
		long senderUIN = UINUtils.make(b.getInt(8), b.getInt(4) & 0xFFFF);
		// the removal of 12 bytes here is to account for the C2E message header
		int messageDataLen = b.getInt(12) - HEADER_C2E_LEN;
		if (messageDataLen > toDecode.length - HEADER_LEN)
			throw new IndexOutOfBoundsException("Not going to work");
		int messageType = b.getInt(28);
		// Do decoding
		ByteBuffer messageDataSlice = PacketReader.wrapLE(toDecode, HEADER_LEN, messageDataLen);
		if (messageType == TYPE_PRAY) {
			return new PackedMessagePRAY(senderUIN, PRAYBlock.read(messageDataSlice, prayMaxDecompressedSize));
		} else if (messageType == TYPE_WRIT) {
			String channel = PacketReader.getString(messageDataSlice);
			int messageId = messageDataSlice.getInt();
			Object p1 = WritVal.readFrom(messageDataSlice);
			Object p2 = WritVal.readFrom(messageDataSlice);
			return new PackedMessageWrit(senderUIN, channel, messageId, p1, p2);
		} else {
			byte[] messageData = new byte[messageDataLen];
			messageDataSlice.get(messageData);
			return new PackedMessageUnknown(senderUIN, messageType, messageData);
		}
	}

	public abstract byte[] getOrPackContents(ConfigMessages msg);

	public byte[] toByteArray(ConfigMessages msg) {
		byte[] messageData = getOrPackContents(msg);
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
