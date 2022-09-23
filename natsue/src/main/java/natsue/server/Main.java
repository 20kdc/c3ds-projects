/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;

import natsue.config.Config;
import natsue.config.IConfigProvider;
import natsue.config.NCFConfigProvider;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.log.StdoutLogProvider;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.jdbc.JDBCNatsueDatabase;
import natsue.server.firewall.ComplexFirewall;
import natsue.server.firewall.FirewallLevel;
import natsue.server.firewall.TrivialFirewall;
import natsue.server.hub.ServerHub;
import natsue.server.hub.SystemUserHubClient;
import natsue.server.packet.SocketThread;
import natsue.server.session.LoginSessionState;

/**
 * It all starts here.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length != 0)
			throw new RuntimeException("Natsue Server expects no parameters.");

		ILogProvider ilp = new StdoutLogProvider();
		ILogSource mySource = ilp.logExtend(Main.class.toString());

		mySource.log("Started logger.");

		Config config = new Config();
		IConfigProvider configProvider = new NCFConfigProvider("ntsuconf.txt");
		config.visit(configProvider);
		configProvider.configFinished();

		mySource.log("Read configuration.");

		INatsueDatabase actualDB = new JDBCNatsueDatabase(ilp, config.db);

		mySource.log("DB abstraction initialized.");

		final ServerHub serverHub = new ServerHub(config, ilp, actualDB);
		// determine the firewall
		switch (config.firewallLevel.getValue()) {
		case minimal:
			mySource.log("Firewall level: minimal: MINIMAL, HAZARDOUS TO VANILLA CLIENTS");
			serverHub.setFirewall(new TrivialFirewall(serverHub));
			break;
		case vanillaSafe:
			mySource.log("Firewall level: vanillaSafe: Should be safe enough.");
			serverHub.setFirewall(new ComplexFirewall(serverHub, false));
			break;
		case full:
		default:
			mySource.log("Firewall level: full: No fun allowed.");
			serverHub.setFirewall(new ComplexFirewall(serverHub, true));
			break;
		}
		// login the system user
		serverHub.clientLogin(new SystemUserHubClient(config, ilp, serverHub), () -> {});

		mySource.log("ServerHub initialized.");

		int port = config.port.getValue();
		try (ServerSocket sv = new ServerSocket(port)) {
			mySource.log("Bound ServerSocket to port " + port + " - ready to accept connections.");

			while (true) {
				Socket skt = sv.accept();
				new SocketThread(skt, (st) -> {
					return new LoginSessionState(config, st, serverHub);
				}, ilp, config).start();
			}
		}
	}
}
