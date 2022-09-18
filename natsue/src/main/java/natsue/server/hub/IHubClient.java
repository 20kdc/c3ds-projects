/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.io.IOException;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;

/**
 * Interface for a client connected to the hub (this means AUTHENTICATED!!!)
 */
public interface IHubClient {
	/**
	 * Returns user data, which includes the UIN.
	 */
	BabelShortUserData getUserData();

	/**
	 * For online/offline notifications.
	 */
	void wwrNotify(boolean online, BabelShortUserData userData);

	/**
	 * Incoming message! Can throw IOException, which counts as failure.
	 */
	void incomingMessage(PackedMessage message) throws IOException;
}
