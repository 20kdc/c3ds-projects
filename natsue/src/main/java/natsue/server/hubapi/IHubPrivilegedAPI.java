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

/**
 * Represents the server.
 */
public interface IHubPrivilegedAPI extends IHubCommonAPI, IHubLoginAPI {
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
	 */
	void sendMessage(long destinationUIN, PackedMessage message, boolean temp);

	/**
	 * Attempts to forcibly disconnect a user by UIN.
	 * Note that this may not work (system users can shrug it off) but regular users are gone.
	 */
	void forceDisconnectUIN(long uin, boolean sync);
}
