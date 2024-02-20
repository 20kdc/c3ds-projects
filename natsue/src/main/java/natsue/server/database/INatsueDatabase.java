/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import java.util.LinkedList;

import natsue.data.babel.UINUtils;

/**
 * Abstract interface to enforce clean separation of SQL-handling code from everything else.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public interface INatsueDatabase {
	/**
	 * Gets a user by UID.
	 * Returns null on failure.
	 */
	NatsueDBUserInfo getUserByUID(int uid);

	/**
	 * Gets a user by UIN.
	 * Returns null on failure.
	 */
	default NatsueDBUserInfo getUserByUIN(long uin) {
		if (UINUtils.isRegularUser(uin))
			return getUserByUID(UINUtils.uid(uin));
		return null;
	}

	/**
	 * Gets a user by nickname.
	 * The nickname is expected to be folded.
	 * Returns null on failure.
	 */
	NatsueDBUserInfo getUserByFoldedNickname(String username);

	/**
	 * Spools a PackedMessage.
	 * Note that a spooled message can only be sent to someone in the database for sanity reasons.
	 */
	boolean spoolMessage(int uid, int causeUID, byte[] pm);

	/**
	 * Removes an arbitrary PackedMessage from a user's spool, or returns null if it's not there.
	 */
	byte[] popFirstSpooledMessage(int uid);

	/**
	 * Registers a creature in the database (or at least tries to...)
	 */
	boolean ensureCreature(String moniker, int firstUID, int ch0, int ch1, int ch2, int ch3, int ch4, String name, String userText);

	/**
	 * Updates a creature's name and user text.
	 * BIG SCARY NOTE: name or userText can be null to omit that field from being updated.
	 */
	boolean updateCreatureText(int senderUID, String moniker, String name, String userText);

	/**
	 * Returns creature info.
	 * Returns null for no info.
	 */
	NatsueDBCreatureInfo getCreatureInfo(String moniker);

	/**
	 * Returns creature events.
	 * Returns null for no events.
	 */
	LinkedList<NatsueDBCreatureEvent> getCreatureEvents(String moniker);

	/**
	 * Registers a creature life event in the database if it does not already exist
	 */
	boolean ensureCreatureEvent(int senderUID, String moniker, int index, int type, int worldTime, int ageTicks, int unixTime, int lifeStage, String param1, String param2, String worldName, String worldID, String userID);

	/**
	 * Returns creatures in world.
	 * Returns null for none.
	 */
	LinkedList<String> getCreaturesInWorld(String worldID, int limit, int offset);

	/**
	 * Returns creatures in world.
	 * Returns null for none.
	 */
	LinkedList<NatsueDBWorldInfo> getWorldsInUser(int uid, int limit, int offset);

	/**
	 * Returns world info.
	 * Returns null for none.
	 */
	NatsueDBWorldInfo getWorldInfo(String id);

	/**
	 * Tries to create a user with the given details.
	 */
	boolean tryCreateUser(NatsueDBUserInfo info);

	/**
	 * Tries to update a user's authentication details.
	 */
	boolean updateUserAuth(int uid, String hash, int flags, long twoFA);
}
