/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.bytestring;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * For high-reliability string conversion.
 */
public abstract class SingleByteCharset extends Charset {
	private final char[] chars = new char[256];
	private final byte[] bytes = new byte[65536];

	public SingleByteCharset(String name, String[] aliases, byte replacement) {
		super(name, null);
		for (int i = 0; i < 65536; i++)
			bytes[i] = replacement;
		for (int i = 0; i < 256; i++) {
			byte b = (byte) i;
			char chr = byteToCharGenerator(b);
			chars[i] = chr;
			bytes[chr] = b;
		}
	}

	public final byte charToByte(char c) {
		return bytes[c];
	}

	public final char byteToChar(byte c) {
		return chars[c & 0xFF];
	}

	protected abstract char byteToCharGenerator(byte c);

	@Override
	public CharsetDecoder newDecoder() {
		return new CharsetDecoder(this, 1, 1) {
			@Override
			protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
				while (in.hasRemaining()) {
					if (!out.hasRemaining())
						return CoderResult.OVERFLOW;
					out.put(chars[in.get() & 0xFF]);
				}
				return CoderResult.UNDERFLOW;
			}
		};
	}

	@Override
	public CharsetEncoder newEncoder() {
		// TODO Auto-generated method stub
		return new CharsetEncoder(this, 1, 1) {
			@Override
			protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
				while (in.hasRemaining()) {
					if (!out.hasRemaining())
						return CoderResult.OVERFLOW;
					out.put(bytes[in.get()]);
				}
				return CoderResult.UNDERFLOW;
			}
		};
	}

}
