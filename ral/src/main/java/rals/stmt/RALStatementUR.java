/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.ScopeContext;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.lex.SrcPos;
import rals.types.RALType;

/**
 * Represents an unresolved statement.
 */
public abstract class RALStatementUR {
	public final SrcPos lineNumber;
	public RALStatementUR(SrcPos ln) {
		lineNumber = ln;
	}

	/**
	 * Resolves an array of expressions.
	 */
	public static RALExpr[] resolveExprs(RALExprUR[] xi, ScopeContext scope) {
		RALExpr[] res = new RALExpr[xi.length];
		for (int i = 0; i < xi.length; i++)
			res[i] = xi[i].resolve(scope);
		return res;
	}

	/**
	 * In types from array of expressions.
	 */
	public static RALType[] inTypesOf(RALExpr[] xi) {
		RALType[] res = new RALType[xi.length];
		for (int i = 0; i < xi.length; i++)
			res[i] = xi[i].inType();
		return res;
	}

	/**
	 * Resolves the statement.
	 */
	public abstract RALStatement resolve(ScopeContext scope);
}
