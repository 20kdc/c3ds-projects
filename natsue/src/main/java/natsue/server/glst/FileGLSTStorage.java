/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.glst;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cdsp.common.data.creaturesarchive.CArcContainer;
import natsue.names.CreatureDataVerifier;

/**
 * GLST storage
 */
public class FileGLSTStorage implements IGLSTStorage {
	public final File rootDir;
	public final boolean compressed;

	public FileGLSTStorage(File file, boolean b) {
		rootDir = file;
		compressed = b;
		file.mkdirs();
	}

	@Override
	public void storeGLST(String moniker, byte[] data) {
		if (!CreatureDataVerifier.verifyMoniker(moniker))
			return;
		try {
			if (!compressed) {
				// Need to decompress first.
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				InputStream ins = CArcContainer.wrapInputStream(new ByteArrayInputStream(data));
				byte[] chk = new byte[4096];
				while (true) {
					int len = ins.read(chk);
					if (len <= 0)
						break;
					baos.write(chk, 0, len);
				}
				doStoreBlob(moniker + ".bin", baos.toByteArray());
			} else {
				doStoreBlob(moniker + ".cra", data);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private synchronized void doStoreBlob(String string, byte[] data) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(new File(rootDir, string))) {
			fos.write(data);
		}
	}
}
