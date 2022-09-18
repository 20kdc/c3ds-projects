/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;
import natsue.data.babel.UINUtils;
import natsue.data.babel.WritVal;
import natsue.log.ILogSource;

/**
 * This client represents a user called System meant to handle fancy tasks.
 */
public class SystemUserHubClient implements IHubClient, ILogSource {
	public final ServerHub hub;
	public final BabelShortUserData userData = new BabelShortUserData("none", "none", "!System", UINUtils.SERVER_UIN);
	public SystemUserHubClient(ServerHub h) {
		hub = h;
	}

	@Override
	public BabelShortUserData getUserData() {
		return userData;
	}

	@Override
	public boolean isSystem() {
		return true;
	}

	@Override
	public void wwrNotify(boolean online, BabelShortUserData theirData) {
		byte[] writ = WritVal.encodeWrit("add_to_contact_book", 2468, UINUtils.toString(userData.uin), null);
		try {
			hub.forceRouteMessage(theirData.uin, new PackedMessage(theirData.uin, PackedMessage.TYPE_WRIT, writ));
		} catch (Exception ex) {
			logTo(hub.log, ex);
		}
		/*
		writ = WritVal.encodeWrit("system_message", 2469, "You didn't say the magic word!", null);
		try {
			hub.forceRouteMessage(theirData.uin, new PackedMessage(theirData.uin, PackedMessage.TYPE_WRIT, writ));
		} catch (Exception ex) {
			logTo(hub.log, ex);
		}*/
	}

	@Override
	public void incomingMessage(PackedMessage message) {
	}
}
