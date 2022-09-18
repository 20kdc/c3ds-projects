/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.packet;

import natsue.config.IConfigProvider;

/**
 * Contains configuration for SocketThread.
 */
public class SocketThreadConfig {
	public boolean logFailedAuthentication;
	public boolean logAllConnections;
	public boolean logAllIncomingPackets;

	public SocketThreadConfig(IConfigProvider icp) {
		logFailedAuthentication = icp.getConfigInt("SocketThreadConfig.logFailedAuthentication", 1) != 0;
		logAllConnections = icp.getConfigInt("SocketThreadConfig.logAllConnections", 1) != 0;
		logAllIncomingPackets = icp.getConfigInt("SocketThreadConfig.logAllIncomingPackets", 1) != 0;
	}
}
