/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Random little IO utilities.
 */
public class IOUtils {
    /**
     * Creates a new little-endian ByteBuffer.
     */
	public static ByteBuffer newBuffer(int size) {
		ByteBuffer res = ByteBuffer.allocate(size);
		res.order(ByteOrder.LITTLE_ENDIAN);
		return res;
	}

	/**
	 * Writes a fixed-length string into a corresponding byte array.
	 */
	public static void setFixedLength(byte[] name, String t, Charset charset) {
		byte[] tmp = t.getBytes(charset);
		Arrays.fill(name, (byte) 0);
		System.arraycopy(tmp, 0, name, 0, tmp.length);
	}

	/**
	 * Gets a fixed-length string from a corresponding byte array.
	 */
	public static String getFixedLength(byte[] name, Charset charset) {
		int len = 0;
		for (int i = 0; i < name.length; i++) {
			if (name[i] == 0)
				break;
			len++;
		}
		return new String(name, 0, len, charset);
	}

    /**
     * Makes sure to read the given amount of bytes from the input stream (no more or less).
     */
    public static void readFully(InputStream socketInput, byte[] data, int ofs, int len) throws IOException {
        int end = ofs + len;
        while (ofs < end) {
            int amount = socketInput.read(data, ofs, end - ofs);
            if (amount <= 0)
                throw new EOFException("Out of data");
            ofs += amount;
        }
    }

    /**
     * Makes sure to read the given amount of bytes from the input stream (no more or less).
     */
    public static byte[] getBytes(InputStream socketInput, int len) throws IOException {
        byte[] data = new byte[len];
        readFully(socketInput, data, 0, len);
        return data;
    }

    /**
     * Makes sure to read the given amount of bytes from the input stream (no more or less).
     * Returns a little-endian ByteBuffer.
     */
    public static ByteBuffer getWrappedBytes(InputStream socketInput, int len) throws IOException {
        return wrapLE(getBytes(socketInput, len));
    }

    /**
     * Converts a byte[] to a little-endian ByteBuffer.
     */
    public static ByteBuffer wrapLE(byte[] total) {
        return wrapLE(total, 0, total.length);
    }

    /**
     * Converts a byte[] to a little-endian ByteBuffer.
     */
    public static ByteBuffer wrapLE(byte[] total, int ofs, int len) {
        ByteBuffer bb = ByteBuffer.wrap(total, ofs, len);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb;
    }

    /**
     * Gets a string with "stream-like" ByteBuffer access
     */
    public static String getString(ByteBuffer bb, Charset charset) {
        int len = bb.getInt();
        byte[] baseArray = bb.array();
        int pos = bb.position();
        String str = new String(baseArray, bb.arrayOffset() + pos, len, charset);
        bb.position(pos + len);
        return str;
    }
}
