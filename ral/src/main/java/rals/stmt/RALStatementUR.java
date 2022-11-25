/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.diag.*;
import rals.lex.DefInfo;

/**
 * Represents an unresolved statement.
 */
public abstract class RALStatementUR {
	public final DefInfo defInfo;
	public final SrcPos lineNumber;
	public final SrcRange extent;

	public RALStatementUR(SrcPos ln) {
		lineNumber = ln;
		extent = ln.toRange();
		defInfo = null;
	}

	public RALStatementUR(SrcRange lr) {
		lineNumber = lr.start;
		extent = lr;
		defInfo = null;
	}

	public RALStatementUR(DefInfo di) {
		extent = di.srcRange;
		lineNumber = extent.start;
		defInfo = di;
	}

	/**
	 * Resolves the statement.
	 */
	public final RALStatement resolve(ScopeContext scope) {
		scope.world.diags.pushFrame(extent);
		RALStatement res = null;
		try {
			scope.world.hcm.resolvePre(extent, scope);
			res = resolveInner(scope);
			scope.world.hcm.resolvePost(extent, scope);
		} catch (Exception ex) {
			scope.world.diags.error("statement resolve: ", ex);
			res = new RALInlineStatement.Resolved(extent, new String[] {"STOP * RAL STATEMENT RESOLVE ERROR"});
		}
		scope.world.diags.popFrame(extent);
		return res;
	}

	/**
	 * Resolves the statement.
	 * Only caller should be resolve, above.
	 */
	protected abstract RALStatement resolveInner(ScopeContext scope);
}
