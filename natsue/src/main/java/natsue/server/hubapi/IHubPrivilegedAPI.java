/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import java.util.LinkedList;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.pm.PackedMessage;
import natsue.server.database.NatsueUserInfo;
import natsue.server.firewall.IRejector;

/**
 * Represents the server.
 */
public interface IHubPrivilegedAPI extends IHubCommonAPI, IHubLoginAPI, IRejector {
	/**
	 * Returns all user info that does not belong to system users.
	 */
	LinkedList<BabelShortUserData> listAllNonSystemUsersOnlineYesIMeanAllOfThem();

	/**
	 * Given a user's username and password, provides a NatsueUserInfo (successful login), or null.
	 * The username will be automatically folded.
	 * Note this will still return the value for frozen accounts.
	 */
	NatsueUserInfo usernameAndPasswordLookup(String username, String password, boolean allowedToRegister);

	/**
	 * Modifies the flags of a user.
	 */
	boolean modUserFlags(long targetUIN, int and, int xor);

	/**
	 * Changes the password of a user.
	 */
	boolean changePassword(long uin, String newPW);

	/**
	 * Adds a client to the system, or returns false if that couldn't happen due to a conflict.
	 * Note that you can't turn back if this returns true, you have to logout again.
	 * The runnable provided here runs at a very specific time such that:
	 * + No functions will quite have been called yet on the client
	 * + The client will definitely be logging in at this point
	 */
	boolean clientLogin(IHubClient client, Runnable confirmOk);

	/**
	 * Route a message that is expected to *eventually* get to the target.
	 * The message is assumed to be authenticated.
	 * If temp is true, the message won't be archived on failure.
	 * If fromRejector is true, then the message won't go through rejection *again*.
	 */
	void sendMessage(long destinationUIN, PackedMessage message, MsgSendType type);

	/**
	 * Attempts to forcibly disconnect a user by UIN.
	 * Note that this may not work (system users can shrug it off) but regular users are gone.
	 */
	void forceDisconnectUIN(long uin, boolean sync);

	/**
	 * Controls message behaviour.
	 */
	public static enum MsgSendType {
		// Chat/etc.
		Temp(false, false),
		// Norns, mail
		Perm(false, true),
		// Rejects
		TempReject(true, false),
		PermReject(true, true);

		public final boolean isReject, shouldSpool;

		MsgSendType(boolean ir, boolean ss) {
			isReject = ir;
			shouldSpool = ss;
		}
	}
}
