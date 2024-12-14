/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.LinkedList;

import cdsp.common.data.IOUtils;

/**
 * IO
 */
public class CS16IO {
	/**
	 * Autodetect CS16 format.
	 */
	public static CS16Format determineFormat(byte[] buffer) {
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int magic = bb.getInt(0);
		for (CS16Format fmt : CS16Format.values())
			if (fmt.magic == magic)
				return fmt;
		throw new RuntimeException("Unknown image type");
	}

	/**
	 * Autodetect CS16 format, then get frame info.
	 */
	public static CS16FrameInfo[] readCS16FrameInfo(File file) throws IOException {
		return readCS16FrameInfo(Files.readAllBytes(file.toPath()));
	}

	/**
	 * Autodetect CS16 format, then get frame info.
	 */
	public static CS16FrameInfo[] readCS16FrameInfo(byte[] buffer) {
		return readCS16FrameInfo(buffer, 0xFFFF, 0xFFFF);
	}

	/**
	 * Autodetect CS16 format, then get frame info. This variant allows a maximum
	 * frame count, rather than the usual implicit 65535-frame bound. This helps
	 * keep allocation under control.
	 */
	public static CS16FrameInfo[] readCS16FrameInfo(byte[] buffer, int maxFrames, int maxCompressedFrameHeight) {
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int magic = bb.getInt(0);
		for (CS16Format fmt : CS16Format.values()) {
			if (fmt.magic != magic)
				continue;
			bb.order(fmt.endian);
			int count = bb.getShort(4) & 0xFFFF;
			if (count > maxFrames)
				throw new RuntimeException("Too many frames");
			CS16FrameInfo[] result = new CS16FrameInfo[count];
			int pos = 6;
			for (int i = 0; i < count; i++) {
				int dptr = bb.getInt(pos);
				int iw = bb.getShort(pos + 4) & 0xFFFF;
				int ih = bb.getShort(pos + 6) & 0xFFFF;
				if (fmt.compressed) {
					if (ih > maxCompressedFrameHeight)
						throw new RuntimeException("Height is above maximum compressed frame height");
					int[] dataOfs = new int[ih];
					dataOfs[0] = dptr;
					for (int j = 1; j < ih; j++)
						dataOfs[j] = bb.getInt(pos + 4 + (4 * j));
					pos += 4 * (ih + 1);
					result[i] = new CS16FrameInfo(bb, fmt, iw, ih, dataOfs);
				} else {
					pos += 8;
					result[i] = new CS16FrameInfo(bb, fmt, iw, ih, new int[] { dptr });
				}
			}
			return result;
		}
		throw new RuntimeException("Unknown image type");
	}

	/**
	 * Autodetect CS16 format, then get frame info and decode.
	 */
	public static S16Image[] decodeCS16(File file) throws IOException {
		return decodeAll(readCS16FrameInfo(file));
	}

	/**
	 * Autodetect CS16 format, then get frame info and decode.
	 */
	public static S16Image[] decodeCS16(byte[] buffer) {
		return decodeAll(readCS16FrameInfo(buffer));
	}

	/**
	 * Decode all CS16FrameInfos into S16Images.
	 */
	public static S16Image[] decodeAll(CS16FrameInfo[] frames) {
		S16Image[] res = new S16Image[frames.length];
		for (int i = 0; i < frames.length; i++)
			res[i] = frames[i].decode();
		return res;
	}

	/**
	 * Encodes an S16/C16.
	 */
	public static byte[] encode(S16Image[] frames, CS16Format format) {
		int headerSize = 6 + (8 * frames.length);
		LinkedList<Integer> pointers = new LinkedList<>();
		if (format.compressed) {
			ByteArrayOutputStream dataSection = new ByteArrayOutputStream();
			for (S16Image si : frames)
				if (si.height > 0)
					headerSize += (si.height - 1) * 4;
			for (S16Image si : frames) {
				pointers.add(headerSize + dataSection.size());
				for (int i = 0; i < si.height; i++) {
					if (i != 0)
						pointers.add(headerSize + dataSection.size());
					try {
						dataSection.write(genC16Row(si, i, format));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				dataSection.write(0);
				dataSection.write(0);
			}
			byte[] data = new byte[headerSize + dataSection.size()];
			ByteBuffer bb = IOUtils.wrapLE(data);
			bb.putInt(format.magic);
			bb.order(format.endian);
			bb.putShort((short) frames.length);
			for (S16Image si : frames) {
				bb.putInt(pointers.removeFirst());
				bb.putShort((short) si.width);
				bb.putShort((short) si.height);
				for (int i = 1; i < si.height; i++)
					bb.putInt(pointers.removeFirst());
			}
			bb.put(dataSection.toByteArray());
			return data;
		} else {
			int dataSectionSize = 0;
			for (S16Image si : frames) {
				pointers.add(headerSize + dataSectionSize);
				dataSectionSize += 2 * si.pixels.length;
			}
			byte[] data = new byte[headerSize + dataSectionSize];
			ByteBuffer bb = IOUtils.wrapLE(data);
			bb.putInt(format.magic);
			bb.order(format.endian);
			bb.putShort((short) frames.length);
			for (S16Image si : frames) {
				bb.putInt(pointers.removeFirst());
				bb.putShort((short) si.width);
				bb.putShort((short) si.height);
			}
			for (S16Image si : frames)
				for (short s : si.pixels)
					bb.putShort(format.colourFormat.from565(s, true));
			return data;
		}
	}

	/**
	 * Generates a C16 row.
	 */
	private static byte[] genC16Row(S16Image image, int y, CS16Format format) {
		int rp = y * image.width;
		int runLength = 0;
		boolean runTransparent = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i = 0; i < image.width; i++) {
			boolean transparent = image.pixels[rp] == 0;
			if (transparent != runTransparent || runLength == 32767) {
				writeC16Run(baos, image.pixels, rp, runLength, runTransparent, format);
				runLength = 1;
				runTransparent = transparent;
			} else {
				runLength++;
			}
			rp++;
		}
		writeC16Run(baos, image.pixels, rp, runLength, runTransparent, format);
		writeShort(baos, 0, format.endian);
		return baos.toByteArray();
	}

	private static void writeC16Run(ByteArrayOutputStream baos, short[] pixels, int rp, int runLength, boolean runTransparent, CS16Format format) {
		if (runLength == 0)
			return;
		int runBase = rp - runLength;
		if (runTransparent) {
			writeShort(baos, runLength << 1, format.endian);
		} else {
			writeShort(baos, (runLength << 1) | 1, format.endian);
			for (int i = 0; i < runLength; i++) {
				short val = pixels[runBase++];
				val = format.colourFormat.from565(val, true);
				writeShort(baos, val, format.endian);
			}
		}
	}

	private static void writeShort(ByteArrayOutputStream baos, int s, ByteOrder endian) {
		if (endian == ByteOrder.LITTLE_ENDIAN) {
			baos.write(s);
			baos.write(s >> 8);
		} else {
			baos.write(s >> 8);
			baos.write(s);
		}
	}

	private static void writeShort(OutputStream baos, int s, ByteOrder endian) throws IOException {
		if (endian == ByteOrder.LITTLE_ENDIAN) {
			baos.write(s);
			baos.write(s >> 8);
		} else {
			baos.write(s >> 8);
			baos.write(s);
		}
	}

	/**
	 * Autodetect CS16 format, then get BLK info.
	 */
	public static BLKInfo readBLKInfo(File file) throws IOException {
		return readBLKInfo(Files.readAllBytes(file.toPath()));
	}

	/**
	 * Autodetect CS16 format, then get BLK info.
	 */
	public static BLKInfo readBLKInfo(byte[] buffer) {
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int magic = bb.getInt(0);
		for (CS16Format fmt : CS16Format.values()) {
			if (fmt.magic != magic)
				continue;
			if (fmt.compressed)
				continue;
			bb.order(fmt.endian);
			int width = bb.getShort(4) & 0xFFFF;
			int height = bb.getShort(6) & 0xFFFF;
			int count = bb.getShort(8) & 0xFFFF;
			return new BLKInfo(bb, fmt, width, height, 10 + (count * 8));
		}
		throw new RuntimeException("Unknown BLK type");
	}

	/**
	 * Encodes a BLK file.
	 */
	public static void encodeBLK(OutputStream os, BLKSource source, CS16Format format) throws IOException {
		int count = source.width * source.height;
		byte[] headBuffer = new byte[10 + (count * 8)];
		ByteBuffer head = IOUtils.wrapLE(headBuffer);
		head.putInt(0, format.magic);
		head.order(format.endian);
		head.putShort(4, (short) source.width);
		head.putShort(6, (short) source.height);
		head.putShort(8, (short) count);
		BLKInfo tmpInfo = new BLKInfo(head, format, source.width, source.height, headBuffer.length);
		int ptr = 10;
		for (int i = 0; i < count; i++) {
			head.putInt(ptr, tmpInfo.getBlockDataOfs(i));
			head.putShort(ptr + 4, (short) BLKSource.BLOCK_SIZE);
			head.putShort(ptr + 6, (short) BLKSource.BLOCK_SIZE);
			ptr += 8;
		}
		// header done
		os.write(headBuffer);
		for (int i = 0; i < count; i++) {
			S16Image img = source.getBlock(i);
			for (int j = 0; j < BLKSource.BLOCK_SIZE * BLKSource.BLOCK_SIZE; j++)
				writeShort(os, format.colourFormat.from565(img.pixels[j], false), format.endian);
		}
	}
}
