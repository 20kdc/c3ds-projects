/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.io.StringWriter;

import rals.code.ScopeContext;
import rals.stmt.RALStatement;

/**
 * Statement expression, used for fancy stuff.
 */
public class RALStmtExpr implements RALExprUR {
	public final RALStatement[] statements;
	public final RALExprUR expr;

	public RALStmtExpr(RALStatement[] st, RALExprUR er) {
		statements = st;
		expr = er;
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		throw new RuntimeException("NYI");
	}
}
