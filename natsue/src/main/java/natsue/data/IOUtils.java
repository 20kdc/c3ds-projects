/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import natsue.data.babel.PacketReader;

public class IOUtils {
	public static ByteBuffer newBuffer(int size) {
		ByteBuffer res = ByteBuffer.allocate(size);
		res.order(ByteOrder.LITTLE_ENDIAN);
		return res;
	}

	public static void setFixedLength(byte[] name, int i, String t) {
		byte[] tmp = t.getBytes(PacketReader.CHARSET);
		Arrays.fill(name, (byte) 0);
		System.arraycopy(tmp, 0, name, 0, tmp.length);
	}

	public static String getFixedLength(byte[] name) {
		int len = 0;
		for (int i = 0; i < name.length; i++) {
			if (name[i] == 0)
				break;
			len++;
		}
		return new String(name, 0, len, PacketReader.CHARSET);
	}

}
