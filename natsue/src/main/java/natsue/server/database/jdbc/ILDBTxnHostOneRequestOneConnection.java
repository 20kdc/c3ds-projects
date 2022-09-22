/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import natsue.config.Config;
import natsue.log.ILogSource;

/**
 * As opposed to strategies like pooling.
 */
public class ILDBTxnHostOneRequestOneConnection implements ILDBTxnHost {
	public final Config config;
	public final ILogSource logSource;

	public ILDBTxnHostOneRequestOneConnection(Config cfg, ILogSource ls) {
		config = cfg;
		logSource = ls;
	}

	@Override
	public AcquiredConnection acquireConnection() throws SQLException {
		final Connection conn = DriverManager.getConnection(config.dbConnection.getValue());
		return new AcquiredConnection() {
			@Override
			public void close() throws Exception {
				conn.close();
			}
			
			@Override
			public Connection getInstance() {
				return conn;
			}
		};
	}

	@Override
	public boolean logExpectedDBErrors() {
		return config.logExpectedDBErrors.getValue();
	}

	@Override
	public ILogSource getLogSource() {
		return logSource;
	}
}
