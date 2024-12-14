/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.nio.ByteBuffer;

/**
 * Similar to CS16FrameInfo, but for BLK files. So entirely different.
 */
public final class BLKInfo extends BLKSource {
	/**
	 * Backing ByteBuffer. It is formally agreed not to seek this ByteBuffer.
	 */
	private final ByteBuffer byteBuffer;

	/**
	 * Block data starts at this offset.
	 * There is a comment in s16.py documenting the (broken) behaviour of a third-party "Creatures 3 Room Editor".
	 * In short, working around the bug requires calculating block offsets ourselves.
	 */
	public final int dataOfs;

	public BLKInfo(ByteBuffer byteBuffer, CS16Format fmt, int w, int h, int dataOfs) {
		super(fmt, w, h);
		this.byteBuffer = byteBuffer;
		this.dataOfs = dataOfs;
	}

	/**
	 * Gets the data offset of a block by index.
	 */
	public int getBlockDataOfs(int index) {
		return dataOfs + (index * BLOCK_SIZE * BLOCK_SIZE * 2);
	}

	@Override
	public S16Image copyBlock(int blockIndex) {
		int blockOfs = getBlockDataOfs(blockIndex);
		S16Image res = new S16Image(BLOCK_SIZE, BLOCK_SIZE);
		for (int i = 0; i < 128 * 128; i++) {
			res.pixels[i] = format.colourFormat.to565(byteBuffer.getShort(blockOfs));
			blockOfs += 2;
		}
		return res;
	}
}
