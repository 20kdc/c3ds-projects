/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.util.HashMap;
import java.util.function.IntConsumer;

import natsue.data.babel.PacketWriter;
import natsue.data.babel.UINUtils;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSClientCommand;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;

/**
 * Used to manage virtual circuit-based pings.
 */
public class PingManager implements ILogSource {
	private final HashMap<Short, IntConsumer> activePings = new HashMap<>();
	private volatile boolean loggedOut = false;
	private volatile short nextVSN = 0;
	private final ISessionClient client;

	public PingManager(ISessionClient c) {
		client = c;
	}

	@Override
	public ILogProvider getLogParent() {
		return client;
	}

	/**
	 * Creates a ping packet.
	 * If this returns null, there's no free slot or the manager is logged out.
	 * A response of 0 is failure, of 1 is success.
	 */
	public byte[] addPing(IntConsumer response) {
		synchronized (this) {
			if (loggedOut)
				return null;
			short vsn = 0;
			for (int i = 0; i < 65536; i++) {
				if (nextVSN != 0) {
					if (!activePings.containsKey(nextVSN)) {
						vsn = nextVSN;
						break;
					}
				}
				nextVSN = (short) (nextVSN + 1);
			}
			if (vsn == 0)
				return null;
			activePings.put(vsn, response);
			if (client.logPings())
				log("Sending: " + vsn);
			return PacketWriter.writeVirtualConnect(UINUtils.SERVER_UIN, vsn);
		}
	}

	/**
	 * Returns true if the packet was accepted by PingManager.
	 */
	public boolean handleResponse(BaseCTOS packet) {
		if (packet instanceof CTOSClientCommand) {
			CTOSClientCommand cc = (CTOSClientCommand) packet;
			if (cc.targetUIN == UINUtils.SERVER_UIN) {
				if (cc.subCommand == 0x0E) {
					// The client accepted the virtual circuit, so of course immediately close the connection
					int serverVSN = (cc.param >> 16) & 0xFFFF;
					IntConsumer activePing;
					synchronized (this) {
						Short serverVSNS = (Short) (short) serverVSN;
						activePing = activePings.get(serverVSNS);
						if (activePing != null)
							activePings.remove(serverVSNS);
					}
					if (activePing != null) {
						if (client.logPings())
							log("Confirmed: " + serverVSN);
						activePing.accept(1);
					}
					try {
						client.sendPacket(PacketWriter.writeVirtualCircuitClose(UINUtils.SERVER_UIN));
					} catch (Exception ex) {
						log(ex);
					}
					return true;
				}
			}
		}
		return false;
	}

	public void logout() {
		synchronized (this) {
			loggedOut = true;
			if (client.logPings())
				if (activePings.size() > 0)
					log("Logout: " + activePings.size() + " outstanding pings");
			for (IntConsumer ping : activePings.values())
				ping.accept(0);
			activePings.clear();
		}
	}
}
