/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.photo;

import java.util.LinkedList;

/**
 * Storage of photos.
 * There's no PhotoFrontend right now, so the verification/sync layer is in here.
 * This is probably not a good thing long-term.
 */
public interface IPhotoStorage {
	/**
	 * Gets a photo PNG, or null if not found.
	 * Verifies moniker and index.
	 */
	byte[] getPhotoPNG(String moniker, int index);

	/**
	 * Gets photo metadata, or null if not found.
	 * Verifies moniker and index.
	 */
	byte[] getPhotoMeta(String moniker, int index);

	/**
	 * Gets event indices for the given moniker.
	 * Will return an empty list on any kind of failure whatsoever.
	 */
	LinkedList<Integer> getIndices(String moniker);

	/**
	 * Checks if the given photo "should exist" (and should NOT be written!!!)
	 * Verifies moniker and index.
	 */
	boolean shouldPhotoExist(String moniker, int index);

	/**
	 * Sets a photo.
	 * Verifies moniker and index.
	 */
	void setPhoto(String moniker, int eventIndex, long senderUIN, byte[] png, int width, int height);
}
