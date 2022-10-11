/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import natsue.server.database.NatsueDBUserInfo;

public class UserInfoRSC implements ILResultSetConverter<NatsueDBUserInfo> {
	public static final UserInfoRSC INSTANCE = new UserInfoRSC();
	public static final String SELECTION = "uid, nickname, nickname_folded, psha256, flags";
	public static final String VALUES = "?, ?, ?, ?, ?";

	@Override
	public NatsueDBUserInfo fromResultSet(ResultSet rs) throws SQLException {
		return new NatsueDBUserInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5));
	}

	@Override
	public void toStatement(NatsueDBUserInfo userInfo, PreparedStatement s) throws SQLException {
		s.setInt(1, userInfo.uid);
		s.setString(2, userInfo.nickname);
		s.setString(3, userInfo.nicknameFolded);
		s.setString(4, userInfo.passwordHash);
		s.setInt(5, userInfo.flags);
	}
}
