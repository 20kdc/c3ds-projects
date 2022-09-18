/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.io.IOException;

import natsue.data.babel.ctos.BaseCTOS;
import natsue.server.hub.IHub;
import natsue.server.hub.IHubClient;

/**
 * This session state is used while connected to the hub.
 */
public class MainSessionState extends BaseSessionState implements IHubClient {
	public final long myUIN;
	public final IHub hub;

	public MainSessionState(ISessionClient c, IHub h, long uin) {
		super(c);
		myUIN = uin;
		hub = h;
	}

	@Override
	public long getUIN() {
		return myUIN;
	}

	@Override
	public void handlePacket(BaseCTOS packet) throws IOException {
		byte[] dummy = packet.makeDummy();
		if (dummy != null)
			client.sendPacket(dummy);
	}

	@Override
	public void logout() {
		hub.logout(this);
	}
}
