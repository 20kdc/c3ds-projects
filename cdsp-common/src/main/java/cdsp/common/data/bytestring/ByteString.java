/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.bytestring;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Immutable byte array.
 */
public final class ByteString implements ByteSequence {
	public static final ByteString EMPTY = new ByteString(ByteSequence.Empty.INSTANCE);

	private final byte[] data;

	/**
	 * ByteString from other thing
	 */
	public ByteString(String data, Charset charset) {
		this.data = data.getBytes(charset);
	}

	/**
	 * ByteString.
	 */
	public ByteString(byte[] data) {
		this.data = data.clone();
	}

	/**
	 * ByteString from byte array slice.
	 */
	public ByteString(byte[] data, int offset, int length) {
		this.data = new byte[length];
		System.arraycopy(data, offset, this.data, 0, length);
	}

	public ByteString(ByteSequence sequence) {
		if (sequence instanceof ByteString) {
			data = ((ByteString) sequence).data;
		} else {
			data = sequence.getBytes();
		}
	}

	@Override
	public int hashCode() {
		int total = 0;
		for (int i = 0; i < data.length; i++) {
			total ^= data[i];
			total <<= 1;
			total ^= total >> 7;
		}
		return total;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ByteString) {
			ByteString other = (ByteString) obj;
			if (other == this)
				return true;
			if (other.data.length != data.length)
				return false;
			for (int i = 0; i < other.data.length; i++)
				if (other.data[i] != data[i])
					return false;
			return true;
		}
		return false;
	}

	@Override
	public byte[] getBytes() {
		return data.clone();
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		System.arraycopy(data, srcBegin, dst, dstBegin, srcEnd - srcBegin);
	}

	@Override
	public byte byteAt(int index) {
		return data[index];
	}

	@Override
	public int length() {
		return data.length;
	}

	@Override
	public String toString(int start, int end, Charset cs) {
		return new String(data, start, end - start, cs);
	}

	public ByteString substring(int start, int end) {
		return new ByteString(data, start, end - start);
	}

	public static final class Builder extends ByteArrayOutputStream implements ByteSequence {
		public Builder() {
		}

		public Builder(ByteSequence sequence) {
			super(sequence.length());
			count = sequence.length();
			sequence.getBytes(0, count, buf, 0);
		}

		public Builder(int capacity) {
			super(capacity);
		}

		@Override
		public byte byteAt(int index) {
			return buf[index];
		}

		/**
		 * Writes ASCII. This should be a constant string, no funny business.
		 */
		public void writeASCII(String string) {
			for (int i = 0; i < string.length(); i++) {
				char chr = string.charAt(i);
				if (chr >= 0x80)
					throw new RuntimeException("writeASCII should only write ASCII");
				write((int) chr);
			}
		}

		@Override
		public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
			if (srcEnd > count)
				throw new IndexOutOfBoundsException();
			System.arraycopy(buf, srcBegin, dst, dstBegin, srcEnd - srcBegin);
		}

		@Override
		public String toString(int start, int end, Charset cs) {
			if (end > count)
				throw new IndexOutOfBoundsException();
			return new String(buf, start, end - start, cs);
		}

		@Override
		public int length() {
			return count;
		}
	}
}
