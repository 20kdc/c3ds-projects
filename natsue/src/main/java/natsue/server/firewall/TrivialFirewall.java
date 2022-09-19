/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;
import natsue.server.hubapi.IHubPrivilegedAPI;

/**
 * Just does the absolute bare minimum: Confirming messages aren't totally faked.
 */
public class TrivialFirewall implements IFirewall {
	public final IHubPrivilegedAPI hub;

	public TrivialFirewall(IHubPrivilegedAPI h) {
		hub = h;
	}

	@Override
	public void wwrNotify(boolean online, BabelShortUserData userData) {
	}

	@Override
	public void handleMessage(BabelShortUserData sourceUser, long destinationUIN, PackedMessage message) {
		if (message.senderUIN != sourceUser.uin)
			return;
		hub.sendMessage(destinationUIN, message, false);
	}
}
