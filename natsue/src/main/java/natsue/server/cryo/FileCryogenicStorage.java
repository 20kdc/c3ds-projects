/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.cryo;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;

import natsue.log.ILogProvider;
import natsue.log.ILogSource;

/**
 * File-based cryogenic creature storage.
 */
public class FileCryogenicStorage implements ICryogenicStorage, ILogSource {
	public final File root;
	public final ILogProvider logParent;
	public FileCryogenicStorage(File baseDir, ILogProvider ilp) {
		baseDir.mkdirs();
		root = baseDir;
		logParent = ilp;
	}
	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	public File getFileByFN(String fn) {
		return new File(root, fn + ".creature");
	}

	@Override
	public long getCryogenicsUsage() {
		long total = 0;
		for (String s : getFilesInCryo()) {
			File f = getFileByFN(s);
			total += f.length();
		}
		return total;
	}

	@Override
	public byte[] readFromCryo(String moniker) {
		File f = getFileByFN(moniker);
		if (!f.exists())
			return null;
		try (FileInputStream fos = new FileInputStream(f)) {
			byte[] bb = new byte[(int) f.length()];
			new DataInputStream(fos).readFully(bb);
			return bb;
		} catch (Exception ex) {
			// kind of expected, really
			return null;
		}
	}

	@Override
	public boolean writeToCryo(String moniker, byte[] data) {
		File f = getFileByFN(moniker);
		try (FileOutputStream fos = new FileOutputStream(f)) {
			fos.write(data);
			return true;
		} catch (Exception ex) {
			log(ex);
			f.delete();
			return false;
		}
	}

	@Override
	public boolean deleteFromCryo(String moniker) {
		return getFileByFN(moniker).delete();
	}

	@Override
	public LinkedList<String> getFilesInCryo() {
		LinkedList<String> lls = new LinkedList<>();
		File[] lst = root.listFiles();
		// uhoh
		if (lst == null)
			return lls;
		for (File f : lst) {
			if (f.isFile()) {
				String name = f.getName();
				if (name.endsWith(".creature"))
					lls.add(name.substring(0, name.length() - 9));
			}
		}
		return lls;
	}
}
