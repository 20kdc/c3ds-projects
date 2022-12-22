/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import natsue.data.babel.pm.PackedMessage;
import natsue.server.userdata.INatsueUserData;

/**
 * Contains the logic to rebound a message that's been rejected.
 * Note that the PackedMessage is assumed to have a checked senderUIN.
 */
public interface IRejector {
	/**
	 * API to reject messages.
	 * This is used by Firewall, SystemUserHubClient, and by the Hub itself.
	 * The destination UIN and sender UIN are unswapped.
	 * The message sender UIN is assumed to be perfectly accurate - they will receive the rejection.
	 */
	public void rejectMessage(long destinationUIN, PackedMessage message, String reason);

	/**
	 * See the other rejectMessage definition.
	 * Note that destUser is just used as a source for the UIN.
	 */
	public default void rejectMessage(INatsueUserData destUser, PackedMessage message, String reason) {
		rejectMessage(destUser.getUIN(), message, reason);
	}
}
