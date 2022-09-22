/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import natsue.log.ILogProvider;
import natsue.log.ILogSource;

public class ILMigrations {
	public static final Migration[] migrations = {
			// 1: create/init version table
			new Migration(null, -1, 1, new String[] {
					"CREATE TABLE natsue_version(version INT)",
					"INSERT INTO natsue_version VALUES (0)"
			}),
			// 6: upgrade to Test3 version
			new Migration(null, 1, 6, new String[] {
					"CREATE TABLE natsue_users(uid INT NOT NULL UNIQUE, nickname TEXT NOT NULL UNIQUE, nickname_folded TEXT NOT NULL UNIQUE, psha256 TEXT, PRIMARY KEY(uid))",
					"CREATE TABLE natsue_spool(id BIGINT NOT NULL, uid INT NOT NULL, data BLOB NOT NULL, PRIMARY KEY(id, uid))",
					"CREATE TABLE natsue_history_creatures(moniker VARCHAR(64) NOT NULL UNIQUE, first_uid INT NOT NULL, ch0 INT NOT NULL, ch1 INT NOT NULL, ch2 INT NOT NULL, ch3 INT NOT NULL, ch4 INT NOT NULL, name TEXT NOT NULL, user_text TEXT NOT NULL, PRIMARY KEY(moniker))",
					"CREATE TABLE natsue_history_events(sender_uid INT NOT NULL, moniker VARCHAR(64) NOT NULL, event_index INT NOT NULL, event_type INT NOT NULL, world_time INT NOT NULL, age_ticks INT NOT NULL, unix_time INT NOT NULL, life_stage INT NOT NULL, param1 TEXT NOT NULL, param2 TEXT NOT NULL, world_name TEXT NOT NULL, world_id TEXT NOT NULL, user_id TEXT NOT NULL, PRIMARY KEY(moniker, event_index))",
					"CREATE UNIQUE INDEX natsue_history_events_index on natsue_history_events(moniker, event_index ASC)"
			}),
	};

	public static void migrate(Connection conn, ILDBVariant variant, ILogProvider ils) throws SQLException {
		ILogSource migrateSource = ils.logExtend("JDBCMigrate");
		// -1 implies even the DB version itself doesn't exist
		int dbVersion = -1;
		Statement workspace = conn.createStatement();
		try (ResultSet rs = workspace.executeQuery("SELECT * FROM natsue_version")) {
			rs.next();
			dbVersion = rs.getInt(1);
		} catch (Exception ex) {
			migrateSource.log("natsue_version table error " + ex + " - DB version assumed to be -1");
		}
		while (true) {
			boolean didAnything = false;
			for (Migration m : migrations) {
				if ((m.fromVer == dbVersion) && ((m.target == null) || (m.target == variant))) {
					migrateSource.log("Performing DB migration " + m.fromVer + " -> " + m.toVer);
					try {
						conn.setAutoCommit(false);
						for (String s : m.statements)
							workspace.execute(s);
						workspace.execute("UPDATE natsue_version SET version=" + m.toVer);
						conn.commit();
					} finally {
						conn.setAutoCommit(true);
					}
					dbVersion = m.toVer;
					didAnything = true;
					break;
				}
			}
			if (!didAnything)
				break;
		}
		workspace.close();
	}

	public static class Migration {
		/**
		 * Target variant, or null for none
		 */
		public final ILDBVariant target;
		public final int fromVer, toVer;
		public final String[] statements;
		public Migration(ILDBVariant targ, int f, int to, String[] stms) {
			target = targ;
			fromVer = f;
			toVer = to;
			statements = stms;
		}
	}
}
