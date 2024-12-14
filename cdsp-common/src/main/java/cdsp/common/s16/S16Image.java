/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * RGB565 image. Note that RGB555 is converted to RGB565 during load.
 */
public class S16Image {
	public final int width, height;
	public final short[] pixels;

	public S16Image(int w, int h) {
		width = w;
		height = h;
		pixels = new short[w * h];
	}

	public BufferedImage toBI(boolean alphaAware) {
		BufferedImage bi = new BufferedImage(width, height,
				alphaAware ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		int[] intpix = new int[pixels.length];
		for (int i = 0; i < pixels.length; i++)
			intpix[i] = CS16ColourFormat.argbFrom565(pixels[i], alphaAware);
		bi.getRaster().setDataElements(0, 0, width, height, intpix);
		return bi;
	}

	/**
	 * Generates a C16 row.
	 */
	public byte[] genC16Row(int y) {
		int rp = y * width;
		int runLength = 0;
		boolean runTransparent = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i = 0; i < width; i++) {
			boolean transparent = pixels[rp] == 0;
			if (transparent != runTransparent || runLength == 32767) {
				writeC16Run(baos, rp, runLength, runTransparent);
				runLength = 1;
				runTransparent = transparent;
			} else {
				runLength++;
			}
			rp++;
		}
		writeC16Run(baos, rp, runLength, runTransparent);
		writeShortLE(baos, 0);
		return baos.toByteArray();
	}

	private void writeC16Run(ByteArrayOutputStream baos, int rp, int runLength, boolean runTransparent) {
		if (runLength == 0)
			return;
		int runBase = rp - runLength;
		if (runTransparent) {
			writeShortLE(baos, runLength << 1);
		} else {
			writeShortLE(baos, (runLength << 1) | 1);
			for (int i = 0 ; i < runLength; i++)
				writeShortLE(baos, pixels[runBase++]);
		}
	}
	private void writeShortLE(ByteArrayOutputStream baos, int s) {
		baos.write(s);
		baos.write(s >> 8);
	}
}
