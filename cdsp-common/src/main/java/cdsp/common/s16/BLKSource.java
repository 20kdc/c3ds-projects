/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

/**
 * Represents a source of BLK blocks.
 */
public abstract class BLKSource {
	public static final int BLOCK_SIZE = 128;

	/**
	 * Format of the frame. This format must not be compressed.
	 */
	public final CS16Format format;

	/**
	 * Width and height; in blocks.
	 */
	public final int width, height;

	public BLKSource(CS16Format fmt, int w, int h) {
		if (fmt.compressed)
			throw new RuntimeException("Compressed format not valid for BLK");
		format = fmt;
		width = w;
		height = h;
	}

	/**
	 * Gets the index of a block.
	 */
	public final int getBlockIndex(int x, int y) {
		return y + (x * height);
	}

	/**
	 * Gets or decodes a block by index.
	 * This may or may not be by-reference.
	 */
	public S16Image getBlock(int index) {
		return copyBlock(index);
	}

	/**
	 * Gets or decodes a block by index.
	 * This is always a copy.
	 */
	public abstract S16Image copyBlock(int index);

	/**
	 * Decodes the entire image.
	 */
	public final S16Image decode() {
		S16Image res = new S16Image(width * BLOCK_SIZE, height * BLOCK_SIZE);
		int blockIndex = 0;
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				res.blit(getBlock(blockIndex++), x * BLOCK_SIZE, y * BLOCK_SIZE, false);
		return res;
	}
}
