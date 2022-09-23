/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import natsue.server.database.INatsueDatabase.UserInfo;

public class UserInfoRSC implements ILResultSetConverter<UserInfo> {
	public static final UserInfoRSC INSTANCE = new UserInfoRSC();
	public static final String SELECTION = "uid, nickname, nickname_folded, psha256";

	@Override
	public UserInfo fromResultSet(ResultSet rs) throws SQLException {
		return new UserInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
	}
}