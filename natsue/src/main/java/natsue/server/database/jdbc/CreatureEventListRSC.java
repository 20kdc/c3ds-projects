/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import natsue.server.database.NatsueDBCreatureEvent;

public class CreatureEventListRSC implements ILResultSetConverter<NatsueDBCreatureEvent> {
	public static final CreatureEventListRSC INSTANCE = new CreatureEventListRSC();
	public static final ILListRSC<NatsueDBCreatureEvent> INSTANCE_LIST = new ILListRSC<NatsueDBCreatureEvent>(INSTANCE);

	public static final String SELECTION =
		"sender_uid, moniker, event_index, " +
		"event_type, world_time, age_ticks, unix_time, life_stage, " +
		"param1, param2, world_name, world_id, user_id, send_unix_time";

	@Override
	public NatsueDBCreatureEvent fromResultSet(ResultSet rs) throws SQLException {
		return new NatsueDBCreatureEvent(
			// senderUID
			rs.getInt(1),
			// moniker
			rs.getString(2),
			// eventIndex
			rs.getInt(3),

			// eventType
			rs.getInt(4),
			// worldTime
			rs.getInt(5),
			// ageTicks
			rs.getInt(6),
			// unixTime
			rs.getInt(7),
			// lifeStage
			rs.getInt(8),

			// param1
			rs.getString(9),
			// param2
			rs.getString(10),
			// worldName
			rs.getString(11),
			// worldID
			rs.getString(12),
			// userID
			rs.getString(13),
			// sendUnixTime
			rs.getLong(14)
		);
	}
}
