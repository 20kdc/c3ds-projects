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

	private File indexToImageFile(String moniker, int eventIndex) {
		return new File(baseDir, moniker + "-" + eventIndex + ".png");
	}

	private File indexToMetaFile(String moniker, int eventIndex) {
		return new File(baseDir, moniker + "-" + eventIndex + ".json");
	}

	@Override
	public synchronized byte[] getPhotoPNG(String moniker, int eventIndex) {
		if (!verifyID(moniker, eventIndex))
			return null;
		try {
			return Files.readAllBytes(indexToImageFile(moniker, eventIndex).toPath());
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
			return Files.readAllBytes(indexToMetaFile(moniker, eventIndex).toPath());
		} catch (Exception e) {
			// let it fail, this is normal
			return null;
		}
	}

	@Override
	public synchronized boolean shouldPhotoExist(String moniker, int eventIndex) {
		if (!verifyID(moniker, eventIndex))
			return false;
		return indexToImageFile(moniker, eventIndex).exists();
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
			File imgFile = indexToImageFile(moniker, eventIndex);
			File metaFile = indexToMetaFile(moniker, eventIndex);
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
