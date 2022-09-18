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

public class CTOSFetchRandomUser extends BaseCTOS {

	@Override
	public String toString() {
		return "CTOSFetchRandomUser";
	}

	/**
	 * Passing 0 here will generate a failure response, as it's an invalid UIN 
	 */
	public byte[] makeResponse(long uin) {
		if (uin == 0)
			return makeDummy();

		byte[] rsp = new byte[32];
		ByteBuffer bb = ByteBuffer.wrap(rsp);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(BASE_FIELD_TICKET, ticketNumber);
		bb.putInt(BASE_FIELD_C, UINUtils.uid(uin));
		bb.putInt(BASE_FIELD_D, UINUtils.hid(uin));
		bb.putInt(BASE_FIELD_E, 1);
		return rsp;
	}

	@Override
	public int transactionDummyLength() {
		return 32;
	}
}
