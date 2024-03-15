/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.bytestring;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Fixed Windows-1252
 */
public final class W1252Fixed extends SingleByteCharset {
	private static final char[] TBL = {
		// 0x80
		0x20AC, 0x0081, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
		// 0x88
		0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x008D, 0x017D, 0x008F,
		// 0x90
		0x0090, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
		// 0x98
		0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x009D, 0x017E, 0x0178,
	};
	public static final W1252Fixed INSTANCE = new W1252Fixed();
	
	private W1252Fixed() {
		super("W1252Fixed", null, (byte) '?');
	}

	@Override
	public boolean contains(Charset cs) {
		return cs.equals(StandardCharsets.US_ASCII);
	}

	@Override
	protected char byteToCharGenerator(byte c) {
		int ci = c & 0xFF;
		// 0x00-0x7F latin-1
		if (ci >= 0x80 && ci < 0xA0)
			return TBL[ci - 0x80];
		// 0xA0-0xFF latin-1
		return (char) ci;
	}
}
