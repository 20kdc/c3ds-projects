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

import cdsp.common.data.IOUtils;
import natsue.config.ConfigMessages;

/**
 * This is used for any packet we don't particularly care about (or don't care about *yet*).
 */
public class CTOSUnknown extends BaseCTOS {
	public int type;
	public final int additionalLength, fixedTransactionDummyLength;
	public final boolean furtherDataFlag;

	public CTOSUnknown(int addLen, int tdl, boolean fdf) {
		additionalLength = addLen;
		fixedTransactionDummyLength = tdl;
		furtherDataFlag = fdf;
	}

	@Override
	public String toString() {
		return "Unknown, type 0x" + Integer.toHexString(type);
	}

	@Override
	public void initializeAndReadRemainder(ConfigMessages pcfg, InputStream inputStream, ByteBuffer initial) throws IOException {
		super.initializeAndReadRemainder(pcfg, inputStream, initial);
		type = initial.getInt(BASE_FIELD_TYPE);
		IOUtils.getBytes(inputStream, additionalLength);
		if (furtherDataFlag) {
			int wantedFurtherData = initial.getInt(BASE_FIELD_FDLEN);
			if (wantedFurtherData < 0 || wantedFurtherData > pcfg.maxUnknownCTOSFurtherDataSize.getValue())
				throw new IOException("Invalid further data!");
		}
	}

	@Override
	public int transactionDummyLength() {
		return fixedTransactionDummyLength;
	}
}
