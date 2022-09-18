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
import java.sql.Statement;
import java.util.Date;

import natsue.IConfigProvider;
import natsue.ILogProvider;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.JDBCNatsueDatabase;

/**
 * It all starts here.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new RuntimeException("Natsue Server expects a single parameter: the JDBC connection path to the database. This can be, for instance, \"jdbc:sqlite:sample.db\".");
		}

		INatsueDatabase configDB = new JDBCNatsueDatabase(DriverManager.getConnection(args[0]));

		INatsueDatabase actualDB = configDB;
		String otherDB = configDB.getConfigString("Main.actualDB", "");
		if (!otherDB.equals(""))
			actualDB = new JDBCNatsueDatabase(DriverManager.getConnection(otherDB));

		ILogProvider ilp = new ILogProvider() {
			@Override
			public void log(String source, String text) {
				System.out.println(new Date() + ": " + source + ": " + text);
			}
		};
		String mySource = Main.class.toString();
		ilp.log(mySource, "Opened connections to DBs and started logger.");

		ServerHub serverHub = new ServerHub(configDB, ilp, actualDB);

		int port = serverHub.config.getConfigInt("Main.port", 49152);
		ServerSocket sv = new ServerSocket(port);
		ilp.log(mySource, "Bound ServerSocket to port " + port + " - ready to accept connections.");
		while (true) {
			Socket skt = sv.accept();
			new SocketThread(skt, serverHub).start();
		}
	}
}
