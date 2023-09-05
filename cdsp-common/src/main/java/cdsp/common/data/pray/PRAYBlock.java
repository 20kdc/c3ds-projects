/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.pray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import cdsp.common.data.IOUtils;

/**
 * Not all into the details, but good enough
 */
public class PRAYBlock {
	public final Charset charset;
	private final byte[] type = new byte[4];
	// mirror of type, keep up to date!
	// exists just as a way to reduce unnecessary conversion.
	private String typeStr;
	public final byte[] name = new byte[128];
	public byte[] data;

	public PRAYBlock(Charset charset) {
		this.charset = charset;
	}

	public PRAYBlock(String string, String str2, byte[] byteArray, Charset charset) {
		this.charset = charset;
		setType(string);
		setName(str2);
		data = byteArray;
	}

	public void setType(String t) {
		IOUtils.setFixedLength(type, t, charset);
		typeStr = t;
	}
	public void setName(String t) {
		IOUtils.setFixedLength(name, t, charset);
	}
	public String getType() {
		return typeStr;
	}
	public String getName() {
		return IOUtils.getFixedLength(name, charset);
	}
	public PRAYBlock copy() {
		return new PRAYBlock(getType(), getName(), data.clone(), charset);
	}

	public static LinkedList<PRAYBlock> copyList(Iterable<PRAYBlock> src) {
		LinkedList<PRAYBlock> blocks = new LinkedList<>();
		for (PRAYBlock blk : src)
			blocks.add(blk.copy());
		return blocks;
	}

	public static LinkedList<PRAYBlock> read(ByteBuffer dataSlice, int maxDecompressedSize, Charset charset) {
		if (dataSlice.get() != (byte) 'P')
			throw new RuntimeException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'R')
			throw new RuntimeException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'A')
			throw new RuntimeException("Not a PRAY file!");
		if (dataSlice.get() != (byte) 'Y')
			throw new RuntimeException("Not a PRAY file!");
		LinkedList<PRAYBlock> blocks = new LinkedList<>();
		int remaining = maxDecompressedSize;
		while (dataSlice.position() != dataSlice.limit()) {
			PRAYBlock pb = readOne(dataSlice, remaining, maxDecompressedSize, charset);
			blocks.add(pb);
			remaining -= pb.data.length;
		}
		return blocks;
	}
	public static PRAYBlock readOne(ByteBuffer dataSlice, int maxBlockSize, int maxTotalSize, Charset charset) {
		PRAYBlock block = new PRAYBlock(charset);
		dataSlice.get(block.type);
		block.typeStr = IOUtils.getFixedLength(block.type, charset);
		dataSlice.get(block.name);
		int compressedDataSize = dataSlice.getInt();
		int decompressedDataSize = dataSlice.getInt();
		int flags = dataSlice.getInt();

		if (decompressedDataSize > maxBlockSize)
			throw new RuntimeException("Too much data in PRAY block: DC:" + decompressedDataSize + ", MS:" + maxBlockSize + ", MT:" + maxTotalSize);

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

	private static PRAYBlockPrepared prepareBlock(PRAYBlock pb, boolean compress) {
		byte[] dataMod = pb.data;
		if (compress) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				DeflaterOutputStream dos = new DeflaterOutputStream(baos);
				dos.write(pb.data);
				dos.close();
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
			dataMod = baos.toByteArray();
			return new PRAYBlockPrepared(pb, pb.data.length, true, dataMod);
		} else {
			return new PRAYBlockPrepared(pb, pb.data.length, false, pb.data);
		}
	}
	public static byte[] write(Iterable<PRAYBlock> blocks, boolean compressPRAYChunks) {
		int totalLen = 4;
		int blockCount = 0;
		for (Iterator<PRAYBlock> iterator = blocks.iterator(); iterator.hasNext(); iterator.next())
			blockCount++;
		if (blockCount == 1)
			return writeFileWithOneBlock(blocks.iterator().next(), compressPRAYChunks);
		PRAYBlockPrepared[] preparedBlocks = new PRAYBlockPrepared[blockCount];
		int blockIndex = 0;
		for (PRAYBlock pb : blocks) {
			PRAYBlockPrepared pbp = prepareBlock(pb, compressPRAYChunks);
			preparedBlocks[blockIndex++] = pbp;
			totalLen += pbp.calcSize();
		}
		ByteBuffer total = IOUtils.newBuffer(totalLen);
		total.put((byte) 'P'); total.put((byte) 'R'); total.put((byte) 'A'); total.put((byte) 'Y');
		for (PRAYBlockPrepared pb : preparedBlocks)
			pb.put(total);
		return total.array();
	}
	public static byte[] writeFileWithOneBlock(PRAYBlock pb, boolean compressPRAYChunks) {
		PRAYBlockPrepared pbp = prepareBlock(pb, compressPRAYChunks);
		int totalLen = 4 + pbp.calcSize();
		ByteBuffer total = IOUtils.newBuffer(totalLen);
		total.put((byte) 'P'); total.put((byte) 'R'); total.put((byte) 'A'); total.put((byte) 'Y');
		pbp.put(total);
		return total.array();
	}
	private static class PRAYBlockPrepared {
		final byte[] type;
		final byte[] name;
		final int fullSize;
		final boolean compressed;
		final byte[] data;
		private PRAYBlockPrepared(PRAYBlock base, int fs, boolean c, byte[] d) {
			type = base.type;
			name = base.name;
			fullSize = fs;
			compressed = c;
			data = d;
		}
		private void put(ByteBuffer total) {
			total.put(type);
			total.put(name);
			total.putInt(data.length);
			total.putInt(fullSize);
			total.putInt(compressed ? 1 : 0);
			total.put(data);
		}
		public int calcSize() {
			return 16 + 128 + data.length;
		}
	}
}
