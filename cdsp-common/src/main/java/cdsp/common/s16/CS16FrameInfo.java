/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.nio.ByteBuffer;

/**
 * S16Image, but the header, seeking information, etc. These are read out by
 * CS16IO. (Writing needs all the line offsets, but that makes reading less
 * secure.) Importantly, these share backing storage (the ByteBuffer).
 */
public final class CS16FrameInfo {
	/**
	 * Backing ByteBuffer. It is formally agreed not to seek this ByteBuffer.
	 */
	private final ByteBuffer byteBuffer;

	/**
	 * Format of the frame.
	 */
	public final CS16Format format;

	/**
	 * Size of the frame.
	 */
	public final int width, height;

	/**
	 * Offsets of the frame data.
	 */
	public final int[] dataOfs;

	public CS16FrameInfo(ByteBuffer byteBuffer, CS16Format fmt, int w, int h, int[] o) {
		this.byteBuffer = byteBuffer;
		this.format = fmt;
		width = w;
		height = h;
		dataOfs = o;
	}

	/**
	 * Finishes decoding the image.
	 */
	public S16Image decode() {
		S16Image s16 = new S16Image(width, height);
		if (format.compressed) {
			int pixIdx = 0;
			for (int row = 0; row < height; row++) {
				int pos = dataOfs[row];
				int expectedEnd = pixIdx + width;
				while (pixIdx < expectedEnd) {
					short elm = byteBuffer.getShort(pos);
					pos += 2;
					if (elm == 0)
						break;
					int runLen = (elm >> 1) & 0x7FFF;
					if ((elm & 1) != 0) {
						for (int idx = 0; idx < runLen; idx++) {
							if (pixIdx == expectedEnd)
								break;
							s16.pixels[pixIdx++] = format.colourFormat.to565(byteBuffer.getShort(pos));
							pos += 2;
						}
					} else {
						pixIdx += runLen;
					}
				}
			}
		} else {
			int pos = dataOfs[0];
			for (int i = 0; i < s16.pixels.length; i++) {
				s16.pixels[i] = format.colourFormat.to565(byteBuffer.getShort(pos));
				pos += 2;
			}
		}
		return s16;
	}
}
