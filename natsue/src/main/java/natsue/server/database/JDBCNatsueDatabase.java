/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import natsue.config.Config;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;

/**
 * JDBC-based Natsue database implementation.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public class JDBCNatsueDatabase implements INatsueDatabase, ILogSource {
	private final ILogProvider logParent;
	private final Connection database;
	private final String stmUserByUID;
	private final String stmUserByNickname;
	private final String stmStoreOnSpool;
	private final String stmDeleteFromSpool;
	private final String stmGetFromSpool;
	private final String stmEnsureCreature;
	private final String stmUpdateCreature;
	private final String stmEnsureCreatureEvent;
	private final String stmCreateUser;
	// Yes, really, I decided this was the best way.
	private final SecureRandom secureRandom = new SecureRandom();
	private final Config config;

	public JDBCNatsueDatabase(ILogProvider ilp, Connection conn, Config cfg) throws SQLException {
		config = cfg;
		database = conn;
		logParent = ilp;
		JDBCMigrate.migrate(database, this);
		stmUserByUID = "SELECT uid, nickname, nickname_folded, psha256 FROM natsue_users WHERE uid=?";
		stmUserByNickname = "SELECT uid, nickname, nickname_folded, psha256 FROM natsue_users WHERE nickname_folded=?";
		stmStoreOnSpool = "INSERT INTO natsue_spool(id, uid, data) VALUES (?, ?, ?)";
		stmDeleteFromSpool = "DELETE FROM natsue_spool WHERE id=? and uid=?";
		stmGetFromSpool = "SELECT id, uid, data FROM natsue_spool WHERE uid=?";
		stmEnsureCreature = "INSERT INTO natsue_history_creatures(moniker, first_uid, ch0, ch1, ch2, ch3, ch4, name, user_text) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		stmUpdateCreature = "UPDATE natsue_history_creatures SET name=?, user_text=? WHERE moniker=?";
		stmEnsureCreatureEvent = "INSERT INTO natsue_history_events(sender_uid, moniker, event_index, event_type, world_time, age_ticks, unix_time, life_stage, param1, param2, world_name, world_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		stmCreateUser = "INSERT INTO natsue_users(uid, nickname, nickname_folded, psha256) VALUES (?, ?, ?, ?)";
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	/**
	 * Before changing this, see stmUserByUID / stmUserByUsername / stmUserByNickname
	 */
	private UserInfo getUserFromResultSet(ResultSet rs) throws SQLException {
		if (!rs.next())
			return null;
		return new UserInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
	}

	@Override
	public UserInfo getUserByUID(int uid) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmUserByUID)) {
				stmt.setInt(1, uid);
				return getUserFromResultSet(stmt.executeQuery());
			} catch (Exception ex) {
				log(ex);
				return null;
			}
		}
	}

	@Override
	public UserInfo getUserByFoldedNickname(String nickname) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmUserByNickname)) {
				stmt.setString(1, nickname);
				return getUserFromResultSet(stmt.executeQuery());
			} catch (Exception ex) {
				log(ex);
				return null;
			}
		}
	}

	@Override
	public void spoolMessage(int uid, byte[] pm) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmStoreOnSpool)) {
				// So my justification for this is that there is no such thing as a portable row ID.
				// With the changes to the constraints I've made, if this is actually causing issues you have other problems.
				stmt.setLong(1, System.currentTimeMillis() ^ secureRandom.nextLong());
				stmt.setInt(2, uid);
				stmt.setBytes(3, pm);
				stmt.executeUpdate();
			} catch (Exception ex) {
				log(ex);
			}
		}
	}

	@Override
	public byte[] popFirstSpooledMessage(int uid) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmGetFromSpool)) {
				stmt.setInt(1, uid);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						long id = rs.getLong(1);
						byte[] message = rs.getBytes(3);
						// and now remove from the spool
						// NOTE: Do not give a message we haven't successfully removed from spool!
						try (PreparedStatement stmt2 = database.prepareStatement(stmDeleteFromSpool)) {
							stmt2.setLong(1, id);
							stmt2.setInt(2, uid);
							stmt2.execute();
						} catch (Exception ex2) {
							// :(
						}
						return message;
					}
				}
			} catch (Exception ex) {
				log(ex);
			}
		}
		return null;
	}

	@Override
	public void ensureCreature(String moniker, int firstUID, int ch0, int ch1, int ch2, int ch3, int ch4, String name, String userText) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmEnsureCreature)) {
				stmt.setString(1, moniker);
				stmt.setInt(2, firstUID);
				stmt.setInt(3, ch0);
				stmt.setInt(4, ch1);
				stmt.setInt(5, ch2);
				stmt.setInt(6, ch3);
				stmt.setInt(7, ch4);
				stmt.setString(8, name);
				stmt.setString(9, userText);
				stmt.executeUpdate();
			} catch (Exception ex) {
				expectedDBError(ex);
				// This is expected to happen, so discard
				try (PreparedStatement stmt = database.prepareStatement(stmUpdateCreature)) {
					stmt.setString(1, name);
					stmt.setString(2, userText);
					stmt.setString(3, moniker);
					stmt.executeUpdate();
				} catch (Exception ex2) {
					// Not expected to happen.
					log(ex2);
				}
			}
		}
	}

	private void expectedDBError(Exception ex) {
		if (config.logExpectedDBErrors.getValue())
			log(ex);
	}

	@Override
	public void ensureCreatureEvent(int senderUID, String moniker, int index, int type, int worldTime, int ageTicks, int unixTime, int lifeStage, String param1, String param2, String worldName, String worldID, String userID) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmEnsureCreatureEvent)) {
				stmt.setInt(1, senderUID);
				stmt.setString(2, moniker);
				stmt.setInt(3, index);
				stmt.setInt(4, type);
				stmt.setInt(5, worldTime);
				stmt.setInt(6, ageTicks);
				stmt.setInt(7, unixTime);
				stmt.setInt(8, lifeStage);
				stmt.setString(9, param1);
				stmt.setString(10, param2);
				stmt.setString(11, worldName);
				stmt.setString(12, worldID);
				stmt.setString(13, userID);
				stmt.executeUpdate();
			} catch (Exception ex) {
				expectedDBError(ex);
			}
		}
	}

	@Override
	public boolean tryCreateUser(UserInfo userInfo) {
		synchronized (this) {
			try (PreparedStatement stmt = database.prepareStatement(stmCreateUser)) {
				stmt.setInt(1, userInfo.uid);
				stmt.setString(2, userInfo.nickname);
				stmt.setString(3, userInfo.nicknameFolded);
				stmt.setString(4, userInfo.passwordHash);
				stmt.executeUpdate();
			} catch (Exception ex) {
				expectedDBError(ex);
				return false;
			}
		}
		return true;
	}
}
