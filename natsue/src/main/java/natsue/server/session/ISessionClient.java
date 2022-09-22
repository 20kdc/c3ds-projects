/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.io.IOException;

import natsue.log.ILogProvider;

/**
 * Represents a connected client.
 */
public interface ISessionClient extends ILogProvider {
	/**
	 * Changes session state.
	 * Must be called from the client's thread.
	 * Passing null here implies a disconnect.
	 */
	public void setSessionState(BaseSessionState sessionState);

	/**
	 * Sends a packet to the client.
	 * Can be called from any thread (a lock will be appropriately held).
	 */
	public void sendPacket(byte[] data) throws IOException;

	/**
	 * Config propagator
	 */
	public boolean logFailedAuth();
	public boolean logPings();

	/**
	 * This is for connection shootdown, so it can disconnect the client from any thread.
	 * If sync is true, clientLogout WILL be performed if necessary before this returns.
	 * Do not enable sync if there is any chance of this being the same thread as the client being booted.
	 */
	public void forceDisconnect(boolean sync);
}
