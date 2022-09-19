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

	public static LinkedList<PRAYBlock> read(ByteBuffer dataSlice, int maxDecompressedSize) throws IOException {
		if (dataSlice.get() != (byte) 'P')
			throw new IOException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'R')
			throw new IOException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'A')
			throw new IOException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'Y')
			throw new IOException("Not a PRAY file!");
		LinkedList<PRAYBlock> blocks = new LinkedList<>();
		while (dataSlice.position() != dataSlice.limit()) {
			PRAYBlock pb = readOne(dataSlice, maxDecompressedSize);
			blocks.add(pb);
			maxDecompressedSize -= pb.data.length;
		}
		return blocks;
	}
	public static PRAYBlock readOne(ByteBuffer dataSlice, int maxBlockSize) throws IOException {
		PRAYBlock block = new PRAYBlock();
		dataSlice.get(block.type);
		dataSlice.get(block.name);
		int compressedDataSize = dataSlice.getInt();
		int decompressedDataSize = dataSlice.getInt();
		int flags = dataSlice.getInt();

		if (decompressedDataSize > maxBlockSize)
			throw new IOException("Too much data in PRAY block!");

		block.data = new byte[maxBlockSize];

		if ((flags & 1) != 0) {
			ByteArrayInputStream bais = new ByteArrayInputStream(dataSlice.array(), dataSlice.arrayOffset() + dataSlice.position(), compressedDataSize);
			InflaterInputStream iis = new InflaterInputStream(bais);
			int pos = 0;
			while (pos < decompressedDataSize) {
				int am = iis.read(block.data, pos, decompressedDataSize - pos);
				if (am <= 0)
					throw new IOException("Ran out of data early");
				pos += am;
			}
			// definitely read it normally honest
			dataSlice.position(dataSlice.position() + compressedDataSize);
		} else {
			dataSlice.get(block.data);
		}
		return block;
	}

	public static byte[] write(Iterable<PRAYBlock> blocks) {
		int totalLen = 4;
		for (PRAYBlock pb : blocks)
			totalLen += 16 + 128 + pb.data.length;
		ByteBuffer total = IOUtils.newBuffer(totalLen);
		total.put((byte) 'P'); total.put((byte) 'R'); total.put((byte) 'A'); total.put((byte) 'Y');
		for (PRAYBlock pb : blocks) {
			total.put(pb.type);
			total.put(pb.name);
			total.putInt(pb.data.length);
			total.putInt(pb.data.length);
			total.putInt(0);
			total.put(pb.data);
		}
		return total.array();
	}
}
