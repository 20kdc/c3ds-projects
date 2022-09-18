/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel.ctos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * This is used for any packet we don't particularly care about (or don't care about *yet*).
 */
public class CTOSUnknown extends BaseCTOS {
	public final int additionalLength, fixedTransactionDummyLength;
	public final boolean furtherDataFlag;

	public CTOSUnknown(int addLen, int tdl, boolean fdf) {
		additionalLength = addLen;
		fixedTransactionDummyLength = tdl;
		furtherDataFlag = fdf;
	}

	@Override
	public void initializeAndReadRemainder(PacketReader pcfg, InputStream inputStream, ByteBuffer initial) throws IOException {
		super.initializeAndReadRemainder(pcfg, inputStream, initial);
		pcfg.getBytes(inputStream, additionalLength, false);
		if (furtherDataFlag) {
			int wantedFurtherData = initial.getInt(BASE_FIELD_FDLEN);
			if (wantedFurtherData < 0 || wantedFurtherData > pcfg.maximumRandomFurtherDataSize)
				throw new IOException("Invalid further data!");
		}
	}

	@Override
	public int transactionDummyLength() {
		return fixedTransactionDummyLength;
	}
}
