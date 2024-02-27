/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.data.creaturesarchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * CreaturesArchive container utilities.
 */
public class CArcContainer {
	private static byte[] MAGIC;
	static {
		String res = "Creatures Evolution Engine - Archived information file. zLib 1.13 compressed.\u001A\u0004";
		MAGIC = res.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Wraps an input stream with an InflaterInputStream, after skipping the
	 * CreaturesArchive header.
	 */
	public static InflaterInputStream wrapInputStream(InputStream outer) throws IOException {
		// magic text
		for (byte b : MAGIC)
			if (outer.read() != b)
				throw new IOException("Did not match CreaturesArchive magic number.");
		return new InflaterInputStream(outer);
	}

	/**
	 * Wraps an output stream with a DeflaterOutputStream, after writing the
	 * CreaturesArchive header. Be sure to properly close the stream.
	 */
	public static OutputStream wrapOutputStream(OutputStream outer) throws IOException {
		outer.write(MAGIC);
		return new DeflaterOutputStream(outer);
	}
}
