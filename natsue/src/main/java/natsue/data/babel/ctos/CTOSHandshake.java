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
import java.nio.charset.StandardCharsets;

import natsue.config.Config;
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;

/**
 * Handshaking
 */
public class CTOSHandshake extends BaseCTOS {
	/**
	 * Username
	 */
	public String username;

	/**
	 * Password
	 */
	public String password;

	public CTOSHandshake() {
	}

	@Override
	public String toString() {
		return "CTOSHandshake[" + username + "]";
	}

	@Override
	public void initializeAndReadRemainder(Config pcfg, InputStream inputStream, ByteBuffer initial) throws IOException {
		super.initializeAndReadRemainder(pcfg, inputStream, initial);
		ByteBuffer hdrExt = PacketReader.getWrappedBytes(inputStream, 20, false);
		int usernameLen = hdrExt.getInt(12);
		int passwordLen = hdrExt.getInt(16);
		int totalLen = usernameLen + passwordLen;
		if (totalLen < 0 || totalLen > pcfg.maxLoginInfoSize.getValue())
			throw new IOException("Invalid message size!");
		byte[] data = PacketReader.getBytes(inputStream, totalLen, false);
		username = new String(data, 0, usernameLen - 1, PacketReader.CHARSET);
		password = new String(data, usernameLen, passwordLen - 1, PacketReader.CHARSET);
	}

	@Override
	public int transactionDummyLength() {
		return 0;
	}
}
