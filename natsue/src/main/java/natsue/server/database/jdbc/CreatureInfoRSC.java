/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import natsue.server.database.NatsueDBCreatureInfo;

public class CreatureInfoRSC implements ILResultSetConverter<NatsueDBCreatureInfo> {
	public static final CreatureInfoRSC INSTANCE = new CreatureInfoRSC();
	public static final String SELECTION = "moniker, first_uid, ch0, ch1, ch2, ch3, ch4, name, user_text";
	public static final String VALUES = "?, ?, ?, ?, ?, ?, ?, ?, ?";

	@Override
	public NatsueDBCreatureInfo fromResultSet(ResultSet rs) throws SQLException {
		return new NatsueDBCreatureInfo(
			// moniker
			rs.getString(1),
			// firstUID
			rs.getInt(2),
			// ch0
			rs.getInt(3),
			// ch1
			rs.getInt(4),
			// ch2
			rs.getInt(5),
			// ch3
			rs.getInt(6),
			// ch4
			rs.getInt(7),
			// name
			rs.getString(8),
			// userText
			rs.getString(9)
		);
	}
}
