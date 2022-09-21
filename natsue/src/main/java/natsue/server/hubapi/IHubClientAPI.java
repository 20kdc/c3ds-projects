/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.pm.PackedMessage;

/**
 * This is the parts of the hub API used by clients.
 */
public interface IHubClientAPI extends IHubCommonAPI {
	/**
	 * Removes a client from the system.
	 */
	void clientLogout(IHubClient cc);

	/**
	 * A client sent a message, what do we do with it?
	 * (Verification happens here.)
	 */
	void clientGiveMessage(IHubClient cc, long destinationUIN, PackedMessage message);

	/**
	 * Client sent creature history
	 */
	void clientSendHistory(IHubClient cc, CreatureHistoryBlob history);
}
