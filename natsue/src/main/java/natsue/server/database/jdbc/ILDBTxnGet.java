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

/**
 * Class for "Get"-style transactions.
 */
public abstract class ILDBTxnGet<V> extends ILDBTxn<V> {
	public final ILResultSetConverter<V> resultSetConverter;
	public final String sql;

	public ILDBTxnGet(ILResultSetConverter<V> rsc, String s) {
		super(true, null);
		resultSetConverter = rsc;
		sql = s;
	}

	@Override
	protected V executeInner(Connection conn) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			parameterize(ps);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return failureResult;
				return resultSetConverter.fromResultSet(rs);
			}
		}
	}

	protected abstract void parameterize(PreparedStatement ps) throws SQLException;
}
