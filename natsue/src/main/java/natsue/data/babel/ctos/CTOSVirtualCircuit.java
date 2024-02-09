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
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;

/**
 * Emulated Virtual Circuits
 */
public class CTOSVirtualCircuit extends BaseCTOS {
	public short sourceVSN;
	public short targetVSN;
	public long targetUIN;
	public byte[] messageData;

	public CTOSVirtualCircuit() {
	}

	@Override
	public String toString() {
		return "CTOSVirtualCircuit {" + sourceVSN + "->" + UINUtils.toString(targetUIN) + ":" + targetVSN + ", " + messageData.length + " bytes}";
	}

	@Override
	public void initializeAndReadRemainder(ConfigMessages pcfg, InputStream inputStream, ByteBuffer initial) throws IOException {
		super.initializeAndReadRemainder(pcfg, inputStream, initial);
		int vsns = initial.getInt(BASE_FIELD_E);
		sourceVSN = (short) (vsns & 0xFFFF);
		targetVSN = (short) ((vsns >> 16) & 0xFFFF);
		ByteBuffer extra = IOUtils.getWrappedBytes(inputStream, 12);
		targetUIN = PacketReader.getUIN(extra, 0);
		int wantedFurtherData = initial.getInt(BASE_FIELD_FDLEN);
		// largest possible NET: WRIT plus metadata
		int maxNetWritPlus = pcfg.maxNetWritSize.getValue() + PackedMessage.HEADER_LEN;
		if (wantedFurtherData < 0 || wantedFurtherData > maxNetWritPlus)
			throw new IOException("Invalid further data!");
		messageData = IOUtils.getBytes(inputStream, wantedFurtherData);
	}

	@Override
	public int transactionDummyLength() {
		return 0;
	}
}
