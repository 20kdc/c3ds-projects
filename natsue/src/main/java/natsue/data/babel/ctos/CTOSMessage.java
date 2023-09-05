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
import natsue.data.babel.UINUtils;

/**
 * Messages!
 */
public class CTOSMessage extends BaseCTOS {
	/**
	 * Which UIN the message is to be sent to.
	 */
	public long targetUIN;
	/**
	 * Message data. This is a Packed Babel Message.
	 * Be sure to overwrite the sender UIN or people can forge messages from anyone.
	 * No prizes for guessing what happens then.
	 */
	public byte[] messageData;

	public CTOSMessage() {
	}

	@Override
	public String toString() {
		return "CTOSMessage{to: " + UINUtils.toString(targetUIN) + ", data:byte[" + messageData.length + "]}";
	}

	@Override
	public void initializeAndReadRemainder(ConfigMessages pcfg, InputStream inputStream, ByteBuffer initial) throws IOException {
		super.initializeAndReadRemainder(pcfg, inputStream, initial);
		int msgDataSize = initial.getInt(BASE_FIELD_FDLEN);
		if (msgDataSize < 0 || msgDataSize > pcfg.maxBabelBinaryMessageSize.getValue())
			throw new IOException("Invalid message size!");
		ByteBuffer data = IOUtils.getWrappedBytes(inputStream, 8);
		targetUIN = UINUtils.make(data.getInt(0), data.getInt(4));
		messageData = IOUtils.getBytes(inputStream, msgDataSize);
	}

	@Override
	public int transactionDummyLength() {
		return 0;
	}
}
