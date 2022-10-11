/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import natsue.data.Snowflake;
import natsue.data.babel.UINUtils;
import natsue.server.database.NatsueDBCreatureEvent;
import natsue.server.database.NatsueDBCreatureInfo;
import natsue.server.database.NatsueDBUserInfo;
import natsue.server.database.NatsueDBWorldInfo;

public class JDBCNatsueTxns {
	public final UserByUID userByUID = new UserByUID();
	public final UserByFoldedNickname userByFoldedNickname = new UserByFoldedNickname();
	public final PopFromSpool popFromSpool = new PopFromSpool();
	public final StoreOnSpool storeOnSpool = new StoreOnSpool();
	public final AddCreature addCreature = new AddCreature();
	public final UpdateCreatureText updateCreatureText = new UpdateCreatureText();
	public final GetCreatureInfo getCreatureInfo = new GetCreatureInfo();
	public final GetCreatureEvents getCreatureEvents = new GetCreatureEvents();
	public final AddCreatureEvent addCreatureEvent = new AddCreatureEvent();
	public final GetCreaturesInWorld getCreaturesInWorld = new GetCreaturesInWorld();
	public final GetWorldsInUser getWorldsInUser = new GetWorldsInUser();
	public final CreateUser createUser = new CreateUser();
	public final UpdateUserAuth updateUserAuth = new UpdateUserAuth();

	public static class UserByUID extends ILDBTxnGet<NatsueDBUserInfo> {
		public int uid;

		public UserByUID() {
			super(UserInfoRSC.INSTANCE, "SELECT " + UserInfoRSC.SELECTION + " FROM natsue_users WHERE uid=?");
		}

		@Override
		protected void parameterize(PreparedStatement ps) throws SQLException {
			ps.setInt(1, uid);
		}
	}
	public static class UserByFoldedNickname extends ILDBTxnGet<NatsueDBUserInfo> {
		public String nicknameFolded;

		public UserByFoldedNickname() {
			super(UserInfoRSC.INSTANCE, "SELECT " + UserInfoRSC.SELECTION + " FROM natsue_users WHERE nickname_folded=?");
		}

		@Override
		protected void parameterize(PreparedStatement ps) throws SQLException {
			ps.setString(1, nicknameFolded);
		}
	}
	public static class PopFromSpool extends ILDBTxn<byte[]> {
		public int uid;

		public PopFromSpool() {
			super(true, null);
		}

		@Override
		protected byte[] executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("SELECT id, uid, data FROM natsue_spool WHERE uid=?")) {
				stmt.setInt(1, uid);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						long id = rs.getLong(1);
						byte[] message = rs.getBytes(3);
						// and now remove from the spool
						// NOTE: Do not give a message we haven't successfully removed from spool!
						try (PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM natsue_spool WHERE id=? and uid=?")) {
							stmt2.setLong(1, id);
							stmt2.setInt(2, uid);
							stmt2.executeUpdate();
							return message;
						}
					}
				}
			}
			return null;
		}
	}
	public static class StoreOnSpool extends ILDBTxn<Boolean> {
		public int uid;
		public byte[] data;

		public StoreOnSpool() {
			super(false, Boolean.FALSE);
		}

		@Override
		protected Boolean executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO natsue_spool(id, uid, data) VALUES (?, ?, ?)")) {
				stmt.setLong(1, Snowflake.generateSnowflake());
				stmt.setInt(2, uid);
				stmt.setBytes(3, data);
				stmt.executeUpdate();
				return Boolean.TRUE;
			}
		}
	}
	public static class AddCreature extends ILDBTxn<Boolean> {
		public String moniker;
		public int firstUID;
		public int ch0;
		public int ch1;
		public int ch2;
		public int ch3;
		public int ch4;
		public String name;
		public String userText;

		public AddCreature() {
			super(true, Boolean.FALSE);
		}

		@Override
		protected Boolean executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO natsue_history_creatures(" +
					"moniker, first_uid, ch0, ch1, ch2, ch3, ch4, name, user_text" +
					") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
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
				return Boolean.TRUE;
			}
		}
	}
	public static class UpdateCreatureText extends ILDBTxn<Boolean> {
		public String moniker;
		public String name;
		public String userText;

		public UpdateCreatureText() {
			super(false, Boolean.FALSE);
		}

		@Override
		protected Boolean executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("UPDATE natsue_history_creatures SET name=?, user_text=? " +
					"WHERE moniker=?")) {
				stmt.setString(1, moniker);
				stmt.setString(2, name);
				stmt.setString(3, userText);
				stmt.executeUpdate();
				return Boolean.TRUE;
			}
		}
	}

	public static class GetCreatureInfo extends ILDBTxnGet<NatsueDBCreatureInfo> {
		public String moniker;

		public GetCreatureInfo() {
			super(CreatureInfoRSC.INSTANCE,
				"SELECT " + CreatureInfoRSC.SELECTION + " FROM natsue_history_creatures " +
				"WHERE moniker=?");
		}

		@Override
		protected void parameterize(PreparedStatement ps) throws SQLException {
			ps.setString(1, moniker);
		}
	}

	public static class GetCreatureEvents extends ILDBTxnGet<LinkedList<NatsueDBCreatureEvent>> {
		public String moniker;

		public GetCreatureEvents() {
			super(CreatureEventListRSC.INSTANCE_LIST,
				"SELECT " + CreatureEventListRSC.SELECTION + " FROM natsue_history_events " +
				"WHERE moniker=? ORDER BY event_index ASC");
		}

		@Override
		protected void parameterize(PreparedStatement ps) throws SQLException {
			ps.setString(1, moniker);
		}
	}

	public static class AddCreatureEvent extends ILDBTxn<Boolean> {
		public int senderUID;
		public String moniker;
		public int eventIndex;
		public int eventType;
		public int worldTime;
		public int ageTicks;
		public int unixTime;
		public int lifeStage;
		public String param1, param2, worldName, worldID, userID;

		public AddCreatureEvent() {
			super(true, Boolean.FALSE);
		}

		@Override
		protected Boolean executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO natsue_history_events(" +
					"sender_uid, moniker, event_index, event_type, world_time, age_ticks, unix_time, life_stage, " +
					"param1, param2, world_name, world_id, user_id" +
					") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
				stmt.setInt(1, senderUID);
				stmt.setString(2, moniker);
				stmt.setInt(3, eventIndex);
				stmt.setInt(4, eventType);
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
				return Boolean.TRUE;
			}
		}
	}

	public static class GetCreaturesInWorld extends ILDBTxnGet<LinkedList<String>> {
		public String worldID;
		public int limit, offset;

		public GetCreaturesInWorld() {
			super(StringRSC.INSTANCE_LIST,
				"SELECT DISTINCT moniker FROM natsue_history_events WHERE world_id=? ORDER BY moniker LIMIT ? OFFSET ?");
		}

		@Override
		protected void parameterize(PreparedStatement ps) throws SQLException {
			ps.setString(1, worldID);
			ps.setInt(2, limit);
			ps.setInt(3, offset);
		}
	}

	public static class GetWorldsInUser extends ILDBTxnGet<LinkedList<NatsueDBWorldInfo>> {
		public int uid;
		public int limit, offset;

		public GetWorldsInUser() {
			super(new ILListRSC<>(new WRSC()),
				"SELECT DISTINCT world_id, sender_uid, world_name FROM natsue_history_events " +
				"WHERE sender_uid=? AND user_id=? ORDER BY world_name LIMIT ? OFFSET ?");
		}

		@Override
		protected void parameterize(PreparedStatement ps) throws SQLException {
			ps.setInt(1, uid);
			ps.setString(2, UINUtils.toString(UINUtils.make(uid, UINUtils.HID_USER)));
			ps.setInt(3, limit);
			ps.setInt(4, offset);
		}
		private static class WRSC implements ILResultSetConverter<NatsueDBWorldInfo> {
			@Override
			public NatsueDBWorldInfo fromResultSet(ResultSet rs) throws SQLException {
				return new NatsueDBWorldInfo(
					// beware the order swap
					rs.getInt(2),
					rs.getString(1),
					rs.getString(3)
				);
			}
		}
	}

	public static class CreateUser extends ILDBTxn<Boolean> {
		public NatsueDBUserInfo userInfo;

		public CreateUser() {
			super(true, Boolean.FALSE);
		}

		@Override
		protected Boolean executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO natsue_users(" + UserInfoRSC.SELECTION + ") VALUES (" + UserInfoRSC.VALUES + ")")) {
				UserInfoRSC.INSTANCE.toStatement(userInfo, stmt);
				stmt.executeUpdate();
				return Boolean.TRUE;
			}
		}
	}
	public static class UpdateUserAuth extends ILDBTxn<Boolean> {
		public int uid;
		public String passwordHash;
		public int flags;

		public UpdateUserAuth() {
			super(true, Boolean.FALSE);
		}

		@Override
		protected Boolean executeInner(Connection conn) throws SQLException {
			try (PreparedStatement stmt = conn.prepareStatement("UPDATE natsue_users SET psha256=?, flags=? WHERE uid=?")) {
				stmt.setString(1, passwordHash);
				stmt.setInt(2, flags);
				stmt.setInt(3, uid);
				stmt.executeUpdate();
				return Boolean.TRUE;
			}
		}
	}
}
