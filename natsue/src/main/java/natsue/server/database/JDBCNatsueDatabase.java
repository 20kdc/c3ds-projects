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
			"CREATE TABLE natsue_users(uid INT NOT NULL UNIQUE, username TEXT NOT NULL UNIQUE, psha256 TEXT, PRIMARY KEY(uid))",
			// 3: create spooled messages table
			"CREATE TABLE natsue_spool(id BIGINT NOT NULL UNIQUE, uid INT NOT NULL, data BLOB NOT NULL, PRIMARY KEY(id))"
	};
	public final ILogProvider log;
	private final Connection database;
	private final PreparedStatement stmUserByUID;
	private final PreparedStatement stmUserByName;
	private final PreparedStatement stmStoreOnSpool;
	private final PreparedStatement stmDeleteFromSpool;
	private final PreparedStatement stmGetFromSpool;

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
		stmUserByUID = conn.prepareStatement("SELECT * FROM natsue_users WHERE uid=?");
		stmUserByName = conn.prepareStatement("SELECT * FROM natsue_users WHERE username=?");
		stmStoreOnSpool = conn.prepareStatement("INSERT INTO natsue_spool(uid, data) VALUES (?, ?)");
		stmDeleteFromSpool = conn.prepareStatement("DELETE FROM natsue_spool WHERE id=? and uid=?");
		stmGetFromSpool = conn.prepareStatement("SELECT * FROM natsue_spool WHERE uid=?");
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
		UserInfo ui = new UserInfo(rs.getString(2), rs.getString(3), rs.getInt(1));
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

	@Override
	public void spoolMessage(int uid, byte[] pm) {
		synchronized (this) {
			try {
				stmStoreOnSpool.setInt(1, uid);
				stmStoreOnSpool.setBytes(2, pm);
				stmStoreOnSpool.executeUpdate();
			} catch (Exception ex) {
				logTo(log, ex);
			}
		}
	}

	@Override
	public byte[] popFirstSpooledMessage(int uid) {
		byte[] message = null;
		synchronized (this) {
			try {
				stmGetFromSpool.setInt(1, uid);
				ResultSet rs = stmGetFromSpool.executeQuery();
				if (rs.next()) {
					long id = rs.getLong(1);
					message = rs.getBytes(3);
					// and now remove from the spool
					stmDeleteFromSpool.setLong(1, id);
					stmDeleteFromSpool.setInt(2, uid);
					stmDeleteFromSpool.execute();
				}
				rs.close();
			} catch (Exception ex) {
				logTo(log, ex);
			}
		}
		return message;
	}
}
