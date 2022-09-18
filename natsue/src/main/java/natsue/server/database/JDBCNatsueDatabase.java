/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import java.sql.Connection;
import java.sql.SQLException;

import natsue.data.babel.UINUtils;

/**
 * JDBC-based Natsue database implementation.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public class JDBCNatsueDatabase implements INatsueDatabase {
	public static final long SERVER_UIN = UINUtils.make(1, 1);
	public static final long TEST_UIN = UINUtils.make(1, 2);

	public JDBCNatsueDatabase(Connection conn) throws SQLException {
		/*
		Statement st = conn.createStatement();
		st.execute("CREATE TABLE users(user VARCHAR NOT NULL UNIQUE, password VARCHAR, uin VARCHAR NOT NULL UNIQUE);");
		conn.close();*/
	}

	@Override
	public String getConfigString(String name, String defaultVal) {
		return defaultVal;
	}

	@Override
	public String getNameByUIN(long uin) {
		if (uin == SERVER_UIN)
			return "Server";
		if (uin == TEST_UIN)
			return "test";
		return null;
	}

	@Override
	public long usernameAndPasswordToUIN(String username, String password) {
		if (username.equals("test"))
			return TEST_UIN;
		return 0;
	}

	@Override
	public long getServerUIN() {
		return SERVER_UIN;
	}
}
