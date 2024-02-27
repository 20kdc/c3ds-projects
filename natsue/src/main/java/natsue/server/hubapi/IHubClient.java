/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import natsue.data.babel.pm.PackedMessage;
import natsue.server.hub.IWWRListener;
import natsue.server.userdata.INatsueUserData;

/**
 * Interface for a client connected to the hub (this means AUTHENTICATED!!!)
 */
public interface IHubClient extends IHubClientAsSeenByOtherClientsPrivileged, IWWRListener, INatsueUserData.Proxy {
	/**
	 * Incoming message!
	 * reject is run if an error occurs.
	 * If reject is null, the message success is not tracked.
	 */
	void incomingMessage(PackedMessage message, Runnable reject, boolean compressIfAllowed);

	/**
	 * Incoming message byte array fast path, for spooled messages.
	 * Returns false if not supported - then the caller must decode the message.
	 * Otherwise returns true and acts as normal.
	 */
	default boolean incomingMessageByteArrayFastPath(byte[] message, Runnable reject) {
		return false;
	}

	/**
	 * This connection is stale or we don't like the client or something.
	 * If sync is true and this returns true, a hub logout call will have completed.
	 * Note that it is possible that this may not return true, specifically for system users.
	 * Note that sync must NOT be true if this is the thread that the client is going to shoot down.
	 * Otherwise, we would join on ourselves - this case is detected but you shouldn't do it.
	 */
	boolean forceDisconnect(boolean sync);

	/**
	 * Marks the client as "not really online". This makes isUINOnline return false.
	 * Called by hub with the hub's lock.
	 */
	void markNotReallyOnline();

	/**
	 * See markNotReallyOnline.
	 * Called by hub with the hub's lock.
	 */
	boolean isNotReallyOnline();
}
