/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import natsue.config.Config;
import natsue.config.IConfigProvider;
import natsue.config.NCFConfigProvider;
import natsue.data.babel.PacketReader;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.JDBCNatsueDatabase;
import natsue.server.firewall.ComplexFirewall;
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

		ILogProvider ilp = new ILogProvider() {
			@Override
			public void log(ILogSource source, String text) {
				while (source != null) {
					text = source + ": " + text;
					ILogProvider parent = source.getLogParent();
					if (parent instanceof ILogSource) {
						source = (ILogSource) parent;
					} else {
						source = null;
					}
				}
				System.out.println(new Date() + ": " + text);
			}
			@Override
			public void log(ILogSource source, Throwable ex) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				log(source, sw.toString());
			}
		};
		ILogSource mySource = ilp.logExtend(Main.class.toString());
		mySource.log("Started logger.");

		Config config = new Config();
		IConfigProvider configProvider = new NCFConfigProvider("ntsuconf.txt");
		config.readInFrom(configProvider);
		configProvider.configFinished();

		INatsueDatabase actualDB = new JDBCNatsueDatabase(ilp, DriverManager.getConnection(config.dbConnection.getValue()), config);

		mySource.log("Opened DB connections.");

		final ServerHub serverHub = new ServerHub(config, ilp, actualDB);
		serverHub.setFirewall(config.complexFirewall.getValue() ? new ComplexFirewall(serverHub) : new TrivialFirewall(serverHub));
		// login the system user
		serverHub.clientLogin(new SystemUserHubClient(config, ilp, serverHub), () -> {});

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
