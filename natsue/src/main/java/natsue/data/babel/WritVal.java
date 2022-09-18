/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.nio.ByteBuffer;

/**
 * Because it happens. 
 */
public class WritVal {
	public static byte[] encodeWrit(String channel, int messageId, Object param1, Object param2) {
		byte[] channelData = channel.getBytes(PacketReader.CHARSET);
		byte[] p1 = toByteArray(param1);
		byte[] p2 = toByteArray(param2);
		ByteBuffer bb = PacketWriter.newBuffer(channelData.length + 8 + p1.length + p2.length);
		bb.putInt(channelData.length);
		bb.put(channelData);
		bb.putInt(messageId);
		bb.put(p1);
		bb.put(p2);
		return bb.array();
	}

	public static Object readFrom(ByteBuffer bb) {
		int i = bb.getInt();
		if (i == 0) {
			return (Integer) bb.getInt();
		} else if (i == 1) {
			return (Float) bb.getFloat();
		} else if (i == 2) {
			int len = bb.getInt();
			byte[] baseArray = bb.array();
			int pos = bb.position();
			String str = new String(baseArray, bb.arrayOffset() + pos, len, PacketReader.CHARSET);
			bb.position(pos + len);
			return str;
		}
		return null;
	}

	public static byte[] toByteArray(Object val) {
		if (val == null) {
			byte[] data = new byte[4];
			data[0] = (byte) 3;
			return data;
		} else if (val instanceof Integer) {
			ByteBuffer bb = PacketWriter.newBuffer(8);
			bb.putInt(4, (Integer) val);
			return bb.array();
		} else if (val instanceof Float) {
			ByteBuffer bb = PacketWriter.newBuffer(8);
			bb.putInt(1);
			bb.putFloat((Float) val);
			return bb.array();
		} else if (val instanceof String) {
			byte[] strDat = ((String) val).getBytes(PacketReader.CHARSET);
			ByteBuffer bb = PacketWriter.newBuffer(strDat.length + 8);
			bb.putInt(2);
			bb.putInt(strDat.length);
			bb.put(strDat);
			return bb.array();
		} else {
			throw new RuntimeException("WritVal can't encode " + val + "!");
		}
	}
}
