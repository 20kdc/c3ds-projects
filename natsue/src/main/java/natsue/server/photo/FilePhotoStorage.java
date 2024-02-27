/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.photo;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;

import cdsp.common.util.UnixTime;
import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.names.CreatureDataVerifier;
import natsue.server.http.JSONEncoder;

/**
 * File access for creature photo data.
 */
public class FilePhotoStorage implements IPhotoStorage, ILogSource {
	public final File baseDir, tempMetaFile, tempImageFile;
	public final ILogProvider logParent;

	public FilePhotoStorage(File baseDir, ILogProvider logParent) {
		this.baseDir = baseDir;
		this.logParent = logParent;
		baseDir.mkdirs();
		// To prevent incomplete data being read.
		this.tempMetaFile = new File(baseDir, ".natsue-temp-meta-file");
		this.tempImageFile = new File(baseDir, ".natsue-temp-image-file");
	}

	private boolean verifyID(String moniker, int index) {
		if (!CreatureDataVerifier.verifyMoniker(moniker))
			return false;
		return index >= 0;
	}

	private synchronized File monikerToDir(String moniker, boolean create) {
		File f = new File(baseDir, moniker);
		if (create)
			f.mkdirs();
		return f;
	}

	@Override
	public LinkedList<Integer> getIndices(String moniker) {
		LinkedList<Integer> list = new LinkedList<>();
		if (!CreatureDataVerifier.verifyMoniker(moniker))
			return list;
		try {
			for (File f : monikerToDir(moniker, false).listFiles()) {
				int idx = verifyEventIndexFile(f);
				if (idx != -1)
					list.add(idx);
			}
		} catch (Exception ex) {
		}
		return list;
	}

	private int verifyEventIndexFile(File f) {
		try {
			if (!f.isFile())
				return -1;
			String name = f.getName();
			if (!name.endsWith(".png"))
				return -1;
			return Integer.parseInt(name.substring(0, name.length() - 4));
		} catch (Exception ex) {
		}
		return -1;
	}

	private File indexToImageFile(File base, int index) {
		return new File(base, index + ".png");
	}

	private File indexToMetaFile(File base, int index) {
		return new File(base, index + ".json");
	}

	@Override
	public synchronized byte[] getPhotoPNG(String moniker, int index) {
		if (!verifyID(moniker, index))
			return null;
		try {
			File monikerDir = monikerToDir(moniker, false);
			return Files.readAllBytes(indexToImageFile(monikerDir, index).toPath());
		} catch (Exception e) {
			// let it fail, this is normal
			return null;
		}
	}

	@Override
	public synchronized byte[] getPhotoMeta(String moniker, int index) {
		if (!verifyID(moniker, index))
			return null;
		try {
			File monikerDir = monikerToDir(moniker, false);
			return Files.readAllBytes(indexToMetaFile(monikerDir, index).toPath());
		} catch (Exception e) {
			// let it fail, this is normal
			return null;
		}
	}

	@Override
	public synchronized boolean shouldPhotoExist(String moniker, int index) {
		if (!verifyID(moniker, index))
			return false;
		File monikerDir = monikerToDir(moniker, false);
		if (!monikerDir.isDirectory())
			return false;
		return indexToImageFile(monikerDir, index).exists();
	}

	@Override
	public synchronized void setPhoto(String moniker, int index, long senderUIN, byte[] png, int width, int height) {
		if (!verifyID(moniker, index))
			return;
		// figure out metadata
		JSONEncoder meta = new JSONEncoder();
		meta.objectStart();
		meta.writeKV("moniker", moniker);
		meta.writeKV("index", index);
		meta.writeKV("senderUIN", UINUtils.toString(senderUIN));
		meta.writeKV("saveTime", UnixTime.get());
		meta.writeKV("width", width);
		meta.writeKV("height", height);
		meta.objectEnd();
		// do the thing
		try {
			File monikerDir = monikerToDir(moniker, true);
			File imgFile = indexToImageFile(monikerDir, index);
			File metaFile = indexToMetaFile(monikerDir, index);
			try (FileOutputStream fos = new FileOutputStream(tempImageFile)) {
				fos.write(png);
			}
			try (FileOutputStream fos = new FileOutputStream(tempMetaFile)) {
				fos.write(meta.out.toString().getBytes(StandardCharsets.UTF_8));
			}
			// potential race condition here for an overwrite
			// as such, we just don't overwrite
			// other possible failures exist, oh well...
			Files.move(tempMetaFile.toPath(), metaFile.toPath());
			Files.move(tempImageFile.toPath(), imgFile.toPath());
		} catch (Exception ex) {
			// Not supposed to happen.
			log(ex);
		}
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

}
