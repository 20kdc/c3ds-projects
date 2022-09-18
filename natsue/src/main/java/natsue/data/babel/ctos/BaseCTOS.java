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

/**
 * Just the obvious bits...
 */
public abstract class BaseCTOS {
	public static final int BASE_FIELD_TYPE = 0;
	public static final int BASE_FIELD_A = 4;
	public static final int BASE_FIELD_B = 8;
	public static final int BASE_FIELD_C = 12;
	public static final int BASE_FIELD_D = 16;
	public static final int BASE_FIELD_TICKET = 20;
	public static final int BASE_FIELD_FDLEN = 24;
	public static final int BASE_FIELD_E = 28;

	public int ticketNumber;

	/**
	 * Called by PacketReader.
	 */
	public void initializeAndReadRemainder(PacketReader pcfg, InputStream inputStream, ByteBuffer initial) throws IOException {
		ticketNumber = initial.getInt(BASE_FIELD_TICKET);
	}

	/**
	 * Should we be worried about this packet needing a response?
	 * If not, then return 0 here.
	 * If we should be worried, return the amount of null bytes with the ticket number thrown in to return.
	 */
	public abstract int transactionDummyLength();

	/**
	 * Creates a dummy response (or null for none).
	 * This is mainly here so that I don't have to think too hard about the virtual circuit response.
	 * VC response is more of an autonomous behaviour.
	 */
	public byte[] makeDummy() {
		int tdl = transactionDummyLength();
		if (tdl != 0) {
			ByteBuffer res = ByteBuffer.allocate(tdl);
			res.order(ByteOrder.LITTLE_ENDIAN);
			res.putInt(BASE_FIELD_TICKET, ticketNumber);
			return res.array();
		}
		return null;
	}
}
