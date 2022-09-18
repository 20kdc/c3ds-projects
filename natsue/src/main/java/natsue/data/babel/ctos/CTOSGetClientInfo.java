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
import java.nio.ByteOrder;

import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;

public class CTOSGetClientInfo extends TargetUIDCTOS {
	@Override
	public String toString() {
		return "CTOSGetClientInfo{of: " + UINUtils.toString(targetUIN) + "}";
	}

	public byte[] makeResponse(byte[] shortUserData) {
		if (shortUserData == null)
			return makeDummy();
		byte[] data = new byte[shortUserData.length + 32];
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(BASE_FIELD_TICKET, ticketNumber);
		bb.putInt(BASE_FIELD_FDLEN, shortUserData.length);
		System.arraycopy(shortUserData, 0, data, 32, shortUserData.length);
		return data;
	}

	@Override
	public int transactionDummyLength() {
		return 32;
	}

}
