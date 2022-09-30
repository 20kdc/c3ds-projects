/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.lex.SrcPos;
import rals.stmt.RALStatement;
import rals.stmt.RALStatementUR;
import rals.types.RALType;

/**
 * Statement expression, used for fancy stuff.
 */
public class RALStmtExpr implements RALExprUR {
	public final RALStatementUR statement;
	public final RALExprUR expr;

	public RALStmtExpr(RALStatementUR st, RALExprUR er) {
		statement = st;
		expr = er;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		ScopeContext sc = new ScopeContext(scope);
		final RALStatement rStmt = statement.resolve(sc);
		final RALExpr rExpr = expr.resolve(sc); 
		return new RALExpr() {
			@Override
			public RALType[] outTypes() {
				return rExpr.outTypes();
			}

			@Override
			public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
				try (CompileContext cc = new CompileContext(context)) {
					rStmt.compile(writer, context);
					rExpr.outCompile(writer, out, context);
				}
			}

			@Override
			public RALType inType() {
				return rExpr.inType();
			}

			@Override
			public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
				try (CompileContext cc = new CompileContext(context)) {
					rStmt.compile(writer, context);
					rExpr.inCompile(writer, input, inputExactType, context);
				}
			}
		};
	}
}
