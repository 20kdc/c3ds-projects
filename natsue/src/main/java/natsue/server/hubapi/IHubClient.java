/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import java.io.IOException;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.pm.PackedMessage;
import natsue.server.hub.IWWRListener;

/**
 * Interface for a client connected to the hub (this means AUTHENTICATED!!!)
 */
public interface IHubClient extends IWWRListener {
	/**
	 * Returns user data, which includes the UIN.
	 */
	BabelShortUserData getUserData();

	/**
	 * Is this a system user (and thus ineligible for random user selection)?
	 * NOTE: This is checked on login and at no other points.
	 */
	boolean isSystem();

	/**
	 * Incoming message!
	 * reject is run if an error occurs.
	 * If reject is null, the message success is not tracked.
	 */
	void incomingMessage(PackedMessage message, Runnable reject);

	/**
	 * This connection is stale or we don't like the client or something.
	 * If this returns true, a hub logout call will have completed.
	 * Note that it is possible that this may not return true, specifically for system users.
	 */
	boolean forceDisconnect();
}
