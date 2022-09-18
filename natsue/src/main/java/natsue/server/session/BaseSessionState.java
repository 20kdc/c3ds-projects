/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.io.IOException;

import natsue.data.babel.ctos.BaseCTOS;

/**
 * Represents a session state.
 * It's assumed the session state got ahold of the IConnectedClient through some means.
 */
public abstract class BaseSessionState {
	public final ISessionClient client;
	public BaseSessionState(ISessionClient c) {
		client = c;
	}

	/**
	 * Handles the given incoming packet.
	 */
	public abstract void handlePacket(BaseCTOS packet) throws IOException;

	/**
	 * For handling tasks that need to be dealt with when the connection closes.
	 */
	public abstract void logout();
}
