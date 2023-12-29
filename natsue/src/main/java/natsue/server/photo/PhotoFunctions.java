/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.photo;

import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteOrder;

import cdsp.common.s16.CS16FrameInfo;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;
import natsue.config.ConfigPhotos;
import natsue.log.ILogSource;

/**
 * Stuff involving photos.
 */
public class PhotoFunctions {
	public static byte[] invalidPhoto;

	public static void ensureResourceInit() {
		try {
			URL res = PhotoFunctions.class.getResource("/invalid_photo.s16");
			URLConnection uc = res.openConnection();
			invalidPhoto = new byte[uc.getContentLength()];
			new DataInputStream(uc.getInputStream()).readFully(invalidPhoto);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Checks that an incoming photo is valid, and if so, decodes it.
	 * Returns null if not.
	 */
	public static S16Image ensureValidPhoto(byte[] photo, ConfigPhotos cfg, ILogSource logger) {
		try {
			CS16FrameInfo[] frames = CS16IO.readCS16FrameInfo(photo, 1);
			// wrong frame count
			if (frames.length != 1) {
				if (cfg.logPhotoErrors.getValue())
					logger.log("photo error: frame count wrong");
				return null;
			}
			if (frames[0].width != 119) {
				if (cfg.logPhotoErrors.getValue())
					logger.log("photo error: frame width not 119");
				return null;
			}
			if (frames[0].height != 139) {
				if (cfg.logPhotoErrors.getValue())
					logger.log("photo error: frame height not 139");
				return null;
			}
			if (frames[0].format.compressed) {
				if (cfg.logPhotoErrors.getValue())
					logger.log("photo error: compressed (it shouldn't be)");
				return null;
			}
			if (frames[0].format.endian != ByteOrder.LITTLE_ENDIAN) {
				if (cfg.logPhotoErrors.getValue())
					logger.log("photo error: wrong endianness");
				return null;
			}
			// and the most important check: does it decode?
			return frames[0].decode();
		} catch (Exception ex) {
			if (cfg.logPhotoErrors.getValue())
				logger.log(ex);
			// nope
			return null;
		}
	}
}
