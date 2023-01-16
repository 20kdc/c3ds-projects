/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.cryo;

import java.util.LinkedList;

/**
 * Or, "A directory where we store creatures".
 * BEWARE:
 * 1. Creatures in cryo are expected to be in .ds.creature format.
 * 2. It's possible for the admin to just, you know, add files without caring about naming.
 * 3. This is not expected to be thread-safe, it's owned by CryoFrontend
 */
public interface ICryogenicStorage {
	/**
	 * Returns the amount of bytes used in cryogenic storage.
	 */
	long getCryogenicsUsage();

	/**
	 * Reads a creature from cryo, or returns null on failure.
	 */
	byte[] readFromCryo(String fileName);

	/**
	 * WARNING: fileName must be verified safe!
	 * Note that fileName does not need ".creature" added to it, this is handled.
	 */
	boolean writeToCryo(String fileName, byte[] data);

	/**
	 * Returns true if the creature was found and deleted from cryo.
	 */
	boolean deleteFromCryo(String fileName);

	/**
	 * Gets a list of monikers in cryogenic storage.
	 * This is a linked list because you'll probably need to do pick-and-remove to filter out geats/etc.
	 */
	LinkedList<String> getFilesInCryo();
}
