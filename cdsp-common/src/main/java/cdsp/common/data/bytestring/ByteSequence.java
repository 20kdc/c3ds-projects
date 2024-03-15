/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.bytestring;

import java.nio.charset.Charset;

/**
 * Byte sequence (to go with ByteString).
 */
public interface ByteSequence {
	/**
	 * Returns the length of this byte sequence.
	 */
	int length();

	/**
	 * Returns a byte.
	 */
	byte byteAt(int index);

	/**
	 * Copies out a slice.
	 */
	void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin);

	/**
	 * Returns a clone of the internal array (if any, otherwise makes one)
	 */
	default byte[] getBytes() {
		byte[] data = new byte[length()];
		getBytes(0, data.length, data, 0);
		return data;
	}

	/**
	 * Converts to a ByteString.
	 */
	default ByteString toByteString() {
		if (length() == 0)
			return ByteString.EMPTY;
		return new ByteString(this);
	}

	/**
	 * Converts to string using the given charset.
	 */
	String toString(int start, int end, Charset cs);

	/**
	 * Converts to string using the given charset.
	 */
	default String toString(Charset cs) {
		return toString(0, length(), cs);
	}

	/**
	 * Returns a copy: start (inclusive), end (exclusive)
	 */
	default ByteSequence subSequence(int start, int end) {
		if (start == 0 && end == length())
			return this;
		if (start == end)
			return Empty.INSTANCE;
		return new ByteSequence.Slice(this, start, end);
	}

	static enum Empty implements ByteSequence {
		INSTANCE;

		@Override
		public int length() {
			return 0;
		}

		@Override
		public byte byteAt(int index) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
			if (srcBegin != 0 || srcEnd != 0)
				throw new IndexOutOfBoundsException();
		}

		@Override
		public String toString(int start, int end, Charset cs) {
			if (start != 0 || end != 0)
				throw new IndexOutOfBoundsException();
			return "";
		}
	}

	static class Slice implements ByteSequence {
		private final ByteSequence backing;
		private final int offset;
		private final int length;

		public Slice(ByteSequence p, int start, int end) {
			backing = p;
			offset = start;
			length = end - start;
			int pLen = p.length();
			if (start < 0 || start > pLen)
				throw new IndexOutOfBoundsException();
			if (end < start || end > pLen)
				throw new IndexOutOfBoundsException();
		}

		@Override
		public int length() {
			return length;
		}

		@Override
		public byte byteAt(int index) {
			if (index < 0 || index >= length)
				throw new IndexOutOfBoundsException();
			return backing.byteAt(index + offset);
		}

		@Override
		public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
			if (srcBegin < 0 || srcBegin > length)
				throw new IndexOutOfBoundsException();
			if (srcEnd < srcBegin || srcEnd > length)
				throw new IndexOutOfBoundsException();
			
		}

		@Override
		public String toString(int offset, int length, Charset cs) {
			if (offset < 0)
				throw new IndexOutOfBoundsException();
			if (length < 0)
				throw new IndexOutOfBoundsException();
			if (offset + length > this.length)
				throw new IndexOutOfBoundsException();
			return toString(this.offset + offset, length, cs);
		}

		@Override
		public ByteSequence subSequence(int start, int end) {
			int nLength = end - start;
			if (nLength < 0)
				throw new IndexOutOfBoundsException();
			if (start < 0 || start > length)
				throw new IndexOutOfBoundsException();
			if (end < 0 || end > length)
				throw new IndexOutOfBoundsException();
			if (start == end)
				return Empty.INSTANCE;
			return new Slice(backing, offset + start, offset + end);
		}
	}
}
