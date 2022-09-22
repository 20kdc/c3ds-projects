/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import natsue.config.Config;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;

/**
 * Represents a rerunnable transaction.
 */
public abstract class ILDBTxn<V> {
	public final boolean failureExpected;
	public final V failureResult;

	public ILDBTxn(boolean failExpected, V failRes) {
		failureExpected = failExpected;
		failureResult = failRes;
	}

	public final V executeOuter(ILDBTxnHost base) {
		try (ILDBTxnHost.AcquiredConnection aConn = base.acquireConnection()) {
			return executeInner(aConn.getInstance());
		} catch (Exception ex) {
			if (base.logExpectedDBErrors() || !failureExpected)
				base.getLogSource().log(ex);
		}
		return failureResult;
	}

	protected abstract V executeInner(Connection conn) throws SQLException;
}
