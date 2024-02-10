/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessageWrit;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.userdata.INatsueUserData;

/**
 * NET: WRIT filtering
 */
public class NetWritFWModule implements IFWModule {
	public final IHubPrivilegedAPI hub;
	public final NetWritLevel level;

	public NetWritFWModule(IHubPrivilegedAPI h, NetWritLevel level) {
		hub = h;
		this.level = level;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public boolean handleMessage(INatsueUserData sourceUser, INatsueUserData destUser, PackedMessage message) {
		if (sourceUser.getUIN() == destUser.getUIN())
			return false;
		if (message instanceof PackedMessageWrit) {
			if (level == NetWritLevel.blocked) {
				hub.rejectMessage(destUser, message, "NET: WRIT not allowed period");
				return true;
			}
			PackedMessageWrit w = (PackedMessageWrit) message;
			if (w.channel.equals("system_message") || w.channel.equals("add_to_contact_book")) {
				hub.rejectMessage(destUser, message, "NET: WRIT on restricted channel");
				return true;
			}
			if (level == NetWritLevel.vanillaSafe) {
				if (w.messageId < 1000) {
					hub.rejectMessage(destUser, message, "NET: WRIT with restricted message ID");
					return true;
				}
			} else if (level == NetWritLevel.restrictive) {
				if (w.messageId != 2468) {
					hub.rejectMessage(destUser, message, "NET: WRIT with restricted message ID");
					return true;
				}
			}
		}
		return false;
	}
}
