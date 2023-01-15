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

import natsue.config.ConfigMessages;
import natsue.data.babel.PacketReader;

public abstract class TargetUIDCTOS extends BaseCTOS {
	public long targetUIN;

	@Override
	public void initializeAndReadRemainder(ConfigMessages pcfg, InputStream inputStream, ByteBuffer initial)
			throws IOException {
		super.initializeAndReadRemainder(pcfg, inputStream, initial);
		targetUIN = PacketReader.getUIN(initial, BASE_FIELD_C);
	}
}
