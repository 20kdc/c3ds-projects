/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.awt.image.BufferedImage;

/**
 * RGB565 image. Note that RGB555 is converted to RGB565 during load. This is a
 * port of the Python libkc3ds's s16.py to Java.
 */
public final class S16Image {
	public final int width, height;
	public final short[] pixels;

	public S16Image(int w, int h) {
		width = w;
		height = h;
		pixels = new short[w * h];
	}

	/**
	 * Creates a copy of this image.
	 */
	public S16Image copy() {
		S16Image theCopy = new S16Image(width, height);
		System.arraycopy(pixels, 0, theCopy.pixels, 0, theCopy.pixels.length);
		return theCopy;
	}

	/**
	 * Gets a specific pixel from the image as a short.
	 * Returns 0 (transparent) if out of range. 
	 */
	public short getPixel(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height)
			return 0;
		return pixels[x + (y * width)];
	}

	/**
	 * Sets a specific pixel from the image as a short.
	 * Does nothing if out of range. 
	 */
	public void putPixel(int x, int y, short value) {
		if (x < 0 || x >= width || y < 0 || y >= height)
			return;
		pixels[x + (y * width)] = value;
	}

	/**
	 * Blits the source image to the given destination.
	 * Note that alpha_aware can be set to false in which case transparency is a lie and so forth.
	 * Also be aware this function won't error if the image goes off the borders.
	 */
	public void blit(S16Image source, int x, int y, boolean alphaAware) {
		int startRow = 0;
		if (y < 0)
			startRow = -y;
		int startCol = 0;
		if (x < 0)
			startCol = -x;
		for (int row = startRow; row < source.height; row++) {
			int rowAdj = row + y;
			if (rowAdj >= height)
				break;
			// alright, now...?
			for (int col = startCol; col < source.width; col++) {
				int colAdj = col + x;
				if (colAdj >= width)
					break;
				short srcPix = source.pixels[col + (row * source.width)];
				if ((!alphaAware) || (srcPix != 0))
					pixels[colAdj + (rowAdj * width)] = srcPix;
			}
		}
	}

	/**
	 * To a BufferedImage for display.
	 */
	public BufferedImage toBI(boolean alphaAware) {
		BufferedImage bi = new BufferedImage(width, height,
				alphaAware ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		int[] intpix = new int[pixels.length];
		for (int i = 0; i < pixels.length; i++)
			intpix[i] = CS16ColourFormat.argbFrom565(pixels[i], alphaAware);
		bi.getRaster().setDataElements(0, 0, width, height, intpix);
		return bi;
	}
}
