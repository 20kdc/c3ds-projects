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
			new Migration(-1, 1,
					"CREATE TABLE natsue_version(version INT)",
					"INSERT INTO natsue_version VALUES (0)"
			),
			// 2: create users table
			new Migration(1, 2,
					"CREATE TABLE natsue_users(uid INT NOT NULL UNIQUE, nickname TEXT NOT NULL UNIQUE, nickname_folded TEXT NOT NULL UNIQUE, psha256 TEXT, PRIMARY KEY(uid))"
			),
			// 3: create spool table
			// NOTE: This contains some MySQL-specific stuff that SQLite is generous enough to accept.
			new Migration(2, 3, "CREATE TABLE natsue_spool(id BIGINT NOT NULL, uid INT NOT NULL, data LONGBLOB NOT NULL, PRIMARY KEY(id, uid))"),
			// 6: creature history, etc.
			new Migration(3, 6,
					"CREATE TABLE natsue_history_creatures(moniker VARCHAR(64) NOT NULL UNIQUE, first_uid INT NOT NULL, ch0 INT NOT NULL, ch1 INT NOT NULL, ch2 INT NOT NULL, ch3 INT NOT NULL, ch4 INT NOT NULL, name TEXT NOT NULL, user_text TEXT NOT NULL, PRIMARY KEY(moniker))",
					"CREATE TABLE natsue_history_events(sender_uid INT NOT NULL, moniker VARCHAR(64) NOT NULL, event_index INT NOT NULL, event_type INT NOT NULL, world_time INT NOT NULL, age_ticks INT NOT NULL, unix_time INT NOT NULL, life_stage INT NOT NULL, param1 TEXT NOT NULL, param2 TEXT NOT NULL, world_name TEXT NOT NULL, world_id TEXT NOT NULL, user_id TEXT NOT NULL, PRIMARY KEY(moniker, event_index))",
					"CREATE UNIQUE INDEX natsue_history_events_index on natsue_history_events(moniker, event_index ASC)"
			),
			// 7: User admin flags
			new Migration(6, 7,
					"ALTER TABLE natsue_users ADD COLUMN flags INT NOT NULL DEFAULT 0"
			),
			// 8: Time extensions
			new Migration(7, 8,
					// Worth noting that this is the "true" sender UID, i.e. the cause of the message.
					// The sender UID stored in the blob may in fact be mucked with by the system (i.e. rejections)
					"ALTER TABLE natsue_spool ADD COLUMN cause_uid INT",
					"ALTER TABLE natsue_spool ADD COLUMN send_unix_time BIGINT",
					"ALTER TABLE natsue_history_events ADD COLUMN send_unix_time BIGINT",
					"ALTER TABLE natsue_history_creatures ADD COLUMN send_unix_time BIGINT",
					"ALTER TABLE natsue_users ADD COLUMN creation_unix_time BIGINT"
			),
			// 9: Additional extensions
			new Migration(8, 9,
					"ALTER TABLE natsue_history_creatures ADD COLUMN updater_name_uid INT",
					"ALTER TABLE natsue_history_creatures ADD COLUMN updater_text_uid INT"
			),
			new Migration(9, 10,
					"ALTER TABLE natsue_users ADD COLUMN two_factor_seed BIGINT"
			),
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
				if (m.fromVer == dbVersion) {
					migrateSource.log("Performing DB migration " + m.fromVer + " -> " + m.toVer);
					for (Fork f : m.forks) {
						if (f.target != null && f.target != variant)
							continue;
						try {
							conn.setAutoCommit(false);
							for (String s : f.statements)
								workspace.execute(s);
							workspace.execute("UPDATE natsue_version SET version=" + m.toVer);
							conn.commit();
						} catch (Exception ex) {
							conn.rollback();
							throw ex;
						} finally {
							conn.setAutoCommit(true);
						}
						dbVersion = m.toVer;
						didAnything = true;
						break;
					}
					if (didAnything) {
						break;
					} else {
						throw new RuntimeException("Migration failure due to no version for this database variant");
					}
				}
			}
			if (!didAnything)
				break;
		}
		workspace.close();
	}

	public static class Migration {
		public final int fromVer, toVer;
		public final Fork[] forks;

		public Migration(int f, int to, Fork... fx) {
			fromVer = f;
			toVer = to;
			forks = fx;
		}

		public Migration(int f, int to, String... fx) {
			fromVer = f;
			toVer = to;
			forks = new Fork[] {new Fork(fx)};
		}
	}

	public static class Fork {
		/**
		 * Target variant, or null for any
		 */
		public final ILDBVariant target;
		public final String[] statements;

		public Fork(String... stms) {
			target = null;
			statements = stms;
		}

		public Fork(ILDBVariant targ, String... stms) {
			target = targ;
			statements = stms;
		}
	}
}
