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

import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.names.CreatureDataVerifier;
import natsue.server.database.UnixTime;
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

	private boolean verifyID(String moniker, int eventIndex) {
		if (!CreatureDataVerifier.verifyMoniker(moniker))
			return false;
		return eventIndex >= 0;
	}

	private synchronized File monikerToDir(String moniker, boolean create) {
		File f = new File(baseDir, moniker);
		if (create)
			f.mkdirs();
		return f;
	}

	@Override
	public LinkedList<Integer> getEventIndices(String moniker) {
		LinkedList<Integer> list = new LinkedList<>();
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

	private File indexToImageFile(File base, int eventIndex) {
		return new File(base, eventIndex + ".png");
	}

	private File indexToMetaFile(File base, int eventIndex) {
		return new File(base, eventIndex + ".json");
	}

	@Override
	public synchronized byte[] getPhotoPNG(String moniker, int eventIndex) {
		if (!verifyID(moniker, eventIndex))
			return null;
		try {
			File monikerDir = monikerToDir(moniker, false);
			return Files.readAllBytes(indexToImageFile(monikerDir, eventIndex).toPath());
		} catch (Exception e) {
			// let it fail, this is normal
			return null;
		}
	}

	@Override
	public synchronized byte[] getPhotoMeta(String moniker, int eventIndex) {
		if (!verifyID(moniker, eventIndex))
			return null;
		try {
			File monikerDir = monikerToDir(moniker, false);
			return Files.readAllBytes(indexToMetaFile(monikerDir, eventIndex).toPath());
		} catch (Exception e) {
			// let it fail, this is normal
			return null;
		}
	}

	@Override
	public synchronized boolean shouldPhotoExist(String moniker, int eventIndex) {
		if (!verifyID(moniker, eventIndex))
			return false;
		File monikerDir = monikerToDir(moniker, false);
		if (!monikerDir.isDirectory())
			return false;
		return indexToImageFile(monikerDir, eventIndex).exists();
	}

	@Override
	public synchronized void setPhoto(String moniker, int eventIndex, long senderUIN, byte[] png, int width, int height) {
		if (!verifyID(moniker, eventIndex))
			return;
		// figure out metadata
		JSONEncoder meta = new JSONEncoder();
		meta.objectStart();
		meta.writeKV("moniker", moniker);
		meta.writeKV("eventIndex", eventIndex);
		meta.writeKV("senderUIN", UINUtils.toString(senderUIN));
		meta.writeKV("saveTime", UnixTime.get());
		meta.writeKV("width", width);
		meta.writeKV("height", height);
		meta.objectEnd();
		// do the thing
		try {
			File monikerDir = monikerToDir(moniker, true);
			File imgFile = indexToImageFile(monikerDir, eventIndex);
			File metaFile = indexToMetaFile(monikerDir, eventIndex);
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
