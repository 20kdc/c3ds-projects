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

import natsue.config.Config;
import natsue.config.IConfigProvider;
import natsue.data.babel.PacketReader;
import natsue.log.ILogProvider;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.JDBCNatsueDatabase;
import natsue.server.hub.ServerHub;
import natsue.server.packet.SocketThread;
import natsue.server.session.LoginSessionState;

/**
 * It all starts here.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new RuntimeException("Natsue Server expects a single parameter: the JDBC connection path to the database. This can be, for instance, \"jdbc:sqlite:sample.db\".");
		}

		ILogProvider ilp = new ILogProvider() {
			@Override
			public void log(String source, String text) {
				System.out.println(new Date() + ": " + source + ": " + text);
			}
			@Override
			public void log(String source, Throwable ex) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				log(source, sw.toString());
			}
		};
		String mySource = Main.class.toString();
		ilp.log(mySource, "Started logger.");

		INatsueDatabase firstDB = new JDBCNatsueDatabase(ilp, DriverManager.getConnection(args[0]));

		Config config = new Config();
		config.readInFrom(firstDB);

		INatsueDatabase actualDB = firstDB;
		String otherDB = config.actualDB.getValue();
		if (!otherDB.equals(""))
			actualDB = new JDBCNatsueDatabase(ilp, DriverManager.getConnection(otherDB));

		ilp.log(mySource, "Opened DB connections.");

		final ServerHub serverHub = new ServerHub(config, ilp, actualDB);

		int port = config.port.getValue();
		try (ServerSocket sv = new ServerSocket(port)) {
			ilp.log(mySource, "Bound ServerSocket to port " + port + " - ready to accept connections.");

			while (true) {
				Socket skt = sv.accept();
				new SocketThread(skt, (st) -> {
					return new LoginSessionState(st, serverHub);
				}, ilp, config).start();
			}
		}
	}
}
