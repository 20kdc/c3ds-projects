/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;

/**
 * JDBC-based Natsue database implementation.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public class JDBCNatsueDatabase implements INatsueDatabase, ILogSource {
	private static final String[] migrations = {
			// 0: create version table
			"CREATE TABLE natsue_version(version INT)",
			// 1: prepare version table
			"INSERT INTO natsue_version VALUES (0)",
			// 2: create users table
			"CREATE TABLE natsue_users(username TEXT NOT NULL UNIQUE, psha256 TEXT, uid INTEGER NOT NULL CHECK(uid != 0) UNIQUE, PRIMARY KEY(uid AUTOINCREMENT))"
	};
	public final ILogProvider log;
	private final Connection database;
	private final PreparedStatement stmUserByUID;
	private final PreparedStatement stmUserByName;

	public JDBCNatsueDatabase(ILogProvider ilp, Connection conn) throws SQLException {
		database = conn;
		log = ilp;
		// -1 implies even the DB version itself doesn't exist
		int dbVersion = -1;
		Statement workspace = conn.createStatement();
		try {
			ResultSet rs = workspace.executeQuery("SELECT * FROM natsue_version");
			rs.next();
			dbVersion = rs.getInt(1);
		} catch (Exception ex) {
			logTo(ilp, "natsue_version table error " + ex + " - DB version assumed to be -1");
		}
		for (int i = dbVersion + 1; i < migrations.length; i++) {
			logTo(ilp, "Performing DB migration " + i);
			workspace.execute(migrations[i]);
			workspace.execute("UPDATE natsue_version SET version=" + i);
		}
		workspace.close();
		stmUserByUID = conn.prepareStatement("SELECT * from natsue_users WHERE uid=?");
		stmUserByName = conn.prepareStatement("SELECT * from natsue_users WHERE username=?");
	}

	@Override
	public String getConfigString(String name, String defaultVal) {
		return defaultVal;
	}

	private UserInfo getUserFromResultSet(ResultSet rs) throws SQLException {
		if (!rs.next()) {
			rs.close();
			return null;
		}
		UserInfo ui = new UserInfo(rs.getString(1), rs.getString(2), rs.getInt(3));
		rs.close();
		return ui;
	}

	@Override
	public UserInfo getUserByUID(int uid) {
		synchronized (this) {
			try {
				stmUserByUID.setInt(1, uid);
				return getUserFromResultSet(stmUserByUID.executeQuery());
			} catch (Exception ex) {
				logTo(log, ex);
				return null;
			}
		}
	}

	@Override
	public UserInfo getUserByUsername(String username) {
		synchronized (this) {
			try {
				stmUserByName.setString(1, username);
				return getUserFromResultSet(stmUserByName.executeQuery());
			} catch (Exception ex) {
				logTo(log, ex);
				return null;
			}
		}
	}
}