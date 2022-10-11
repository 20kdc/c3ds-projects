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
import java.util.LinkedList;

/**
 * List converter
 */
public class ILListRSC<V> implements ILResultSetConverter<LinkedList<V>> {
	public final ILResultSetConverter<V> base;
	public ILListRSC(ILResultSetConverter<V> b) {
		base = b;
	}

	@Override
	public LinkedList<V> fromResultSet(ResultSet rs) throws SQLException {
		LinkedList<V> res = new LinkedList<>();
		res.add(base.fromResultSet(rs));
		while (rs.next())
			res.add(base.fromResultSet(rs));
		return res;
	}
}
