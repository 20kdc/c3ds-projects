/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import natsue.config.IConfigProvider;
import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;

/**
 * Abstract interface to enforce clean separation of SQL-handling code from everything else.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public interface INatsueDatabase {
	/**
	 * Gets a user by UID.
	 * Returns null on failure.
	 */
	NatsueUserInfo getUserByUID(int uid);

	/**
	 * Gets a user by UIN.
	 * Returns null on failure.
	 */
	default NatsueUserInfo getUserByUIN(long uin) {
		if (UINUtils.hid(uin) == UINUtils.HID_USER)
			return getUserByUID(UINUtils.uid(uin));
		return null;
	}

	/**
	 * Gets a user by nickname.
	 * The nickname is expected to be folded.
	 * Returns null on failure.
	 */
	NatsueUserInfo getUserByFoldedNickname(String username);

	/**
	 * Spools a PackedMessage.
	 * Note that a spooled message can only be sent to someone in the database for sanity reasons.
	 */
	boolean spoolMessage(int uid, byte[] pm);

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
	 */
	boolean updateCreatureText(String moniker, String name, String userText);

	/**
	 * Registers a creature life event in the database if it does not already exist
	 */
	boolean ensureCreatureEvent(int senderUID, String moniker, int index, int type, int worldTime, int ageTicks, int unixTime, int lifeStage, String param1, String param2, String worldName, String worldID, String userID);

	/**
	 * Tries to create a user with the given details.
	 */
	boolean tryCreateUser(NatsueUserInfo info);
}
