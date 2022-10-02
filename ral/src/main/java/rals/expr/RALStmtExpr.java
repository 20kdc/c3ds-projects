/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.stmt.*;
import rals.types.*;

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
	public RALExprSlice resolve(ScopeContext scope) {
		ScopeContext sc = new ScopeContext(scope);
		final RALStatement rStmt = statement.resolve(sc);
		final RALExprSlice rExpr = expr.resolve(sc); 
		return new Resolved(rStmt, rExpr);
	}

	public static final class Resolved extends RALExprSlice {
		private final RALStatement rStmt;
		private final RALExprSlice rExpr;

		public Resolved(RALStatement rStmt, RALExprSlice rExpr) {
			super(rExpr.length);
			this.rStmt = rStmt;
			this.rExpr = rExpr;
		}

		@Override
		protected RALExprSlice sliceInner(int base, int length) {
			return new Resolved(rStmt, rExpr.slice(base, length));
		}

		@Override
		protected RALType readTypeInner(int index) {
			return rExpr.readType(index);
		}

		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			try (CompileContext cc = new CompileContext(context)) {
				rStmt.compile(context.writer, context);
				rExpr.readCompile(out, context);
			}
		}

		@Override
		protected RALType writeTypeInner(int index) {
			return rExpr.writeType(index);
		}

		@Override
		protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
			try (CompileContext cc = new CompileContext(context)) {
				rStmt.compile(context.writer, context);
				rExpr.writeCompile(index, input, inputExactType, context);
			}
		}
	}
}
