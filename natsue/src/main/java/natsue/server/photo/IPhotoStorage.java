/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.photo;

/**
 * Storage of photos.
 * There's no PhotoFrontend right now, so the verification/sync layer is in here.
 * This is probably not a good thing long-term.
 */
public interface IPhotoStorage {
	/**
	 * Gets a photo PNG, or null if not found.
	 * Verifies moniker and eventIndex.
	 */
	byte[] getPhotoPNG(String moniker, int eventIndex);

	/**
	 * Gets photo metadata, or null if not found.
	 * Verifies moniker and eventIndex.
	 */
	byte[] getPhotoMeta(String moniker, int eventIndex);

	/**
	 * Checks if the given photo "should exist" (and should NOT be written!!!)
	 * Verifies moniker and eventIndex.
	 */
	boolean shouldPhotoExist(String moniker, int eventIndex);

	/**
	 * Sets a photo.
	 * Verifies moniker and eventIndex.
	 */
	void setPhoto(String moniker, int eventIndex, long senderUIN, byte[] png, int width, int height);
}
