/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.pray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.zip.InflaterInputStream;

import natsue.data.IOUtils;

/**
 * Not all into the details, but good enough
 */
public class PRAYBlock {
	public final byte[] type = new byte[4];
	public final byte[] name = new byte[128];
	public byte[] data;

	public PRAYBlock() {
		
	}

	public PRAYBlock(String string, String str2, byte[] byteArray) {
		setType(string);
		setName(str2);
		data = byteArray;
	}

	public void setType(String t) {
		IOUtils.setFixedLength(type, 4, t);
	}
	public void setName(String t) {
		IOUtils.setFixedLength(name, 128, t);
	}
	public String getType() {
		return IOUtils.getFixedLength(type);
	}
	public String getName() {
		return IOUtils.getFixedLength(name);
	}

	public int calcSize() {
		return 16 + 128 + data.length;
	}

	public static LinkedList<PRAYBlock> read(ByteBuffer dataSlice, int maxDecompressedSize) {
		if (dataSlice.get() != (byte) 'P')
			throw new RuntimeException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'R')
			throw new RuntimeException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'A')
			throw new RuntimeException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'Y')
			throw new RuntimeException("Not a PRAY file!");
		LinkedList<PRAYBlock> blocks = new LinkedList<>();
		while (dataSlice.position() != dataSlice.limit()) {
			PRAYBlock pb = readOne(dataSlice, maxDecompressedSize);
			blocks.add(pb);
			maxDecompressedSize -= pb.data.length;
		}
		return blocks;
	}
	public static PRAYBlock readOne(ByteBuffer dataSlice, int maxBlockSize) {
		PRAYBlock block = new PRAYBlock();
		dataSlice.get(block.type);
		dataSlice.get(block.name);
		int compressedDataSize = dataSlice.getInt();
		int decompressedDataSize = dataSlice.getInt();
		int flags = dataSlice.getInt();

		if (decompressedDataSize > maxBlockSize)
			throw new RuntimeException("Too much data in PRAY block!");

		block.data = new byte[decompressedDataSize];

		if ((flags & 1) != 0) {
			ByteArrayInputStream bais = new ByteArrayInputStream(dataSlice.array(), dataSlice.arrayOffset() + dataSlice.position(), compressedDataSize);
			InflaterInputStream iis = new InflaterInputStream(bais);
			try {
				int pos = 0;
				while (pos < decompressedDataSize) {
					int am = iis.read(block.data, pos, decompressedDataSize - pos);
					if (am <= 0)
						throw new RuntimeException("Ran out of data early");
					pos += am;
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
			// definitely read it normally honest
			dataSlice.position(dataSlice.position() + compressedDataSize);
		} else {
			dataSlice.get(block.data);
		}
		return block;
	}

	private static void putBlock(ByteBuffer total, PRAYBlock pb) {
		total.put(pb.type);
		total.put(pb.name);
		total.putInt(pb.data.length);
		total.putInt(pb.data.length);
		total.putInt(0);
		total.put(pb.data);
	}
	public static byte[] write(Iterable<PRAYBlock> blocks) {
		int totalLen = 4;
		for (PRAYBlock pb : blocks)
			totalLen += pb.calcSize();
		ByteBuffer total = IOUtils.newBuffer(totalLen);
		total.put((byte) 'P'); total.put((byte) 'R'); total.put((byte) 'A'); total.put((byte) 'Y');
		for (PRAYBlock pb : blocks)
			putBlock(total, pb);
		return total.array();
	}
	public static byte[] writeFileWithOneBlock(PRAYBlock pb) {
		int totalLen = 4 + pb.calcSize();
		ByteBuffer total = IOUtils.newBuffer(totalLen);
		total.put((byte) 'P'); total.put((byte) 'R'); total.put((byte) 'A'); total.put((byte) 'Y');
		putBlock(total, pb);
		return total.array();
	}
}