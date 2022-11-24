/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.diag.*;

/**
 * Represents an unresolved statement.
 */
public abstract class RALStatementUR {
	public final SrcPos lineNumber;
	public final SrcRange extent;

	public RALStatementUR(SrcPos ln) {
		lineNumber = ln;
		extent = ln.toRange();
	}

	public RALStatementUR(SrcRange lr) {
		lineNumber = lr.start;
		extent = lr;
	}

	/**
	 * Resolves the statement.
	 */
	public final RALStatement resolve(ScopeContext scope) {
		try {
			scope.world.hcm.statementResolvePre(this, scope);
			RALStatement res = resolveInner(scope);
			scope.world.hcm.statementResolvePost(this, scope);
			return res;
		} catch (Exception ex) {
			scope.world.diags.error(extent, "statement resolve: ", ex);
			return new RALInlineStatement.Resolved(extent, new String[] {"STOP * RAL STATEMENT RESOLVE ERROR"});
		}
	}

	/**
	 * Resolves the statement.
	 * Only caller should be resolve, above.
	 */
	protected abstract RALStatement resolveInner(ScopeContext scope);
}
