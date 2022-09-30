/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.cond.RALCondition;
import rals.types.RALType;

/**
 * Conditional ternary.
 */
public class RALTernary implements RALExprUR {
	public final RALCondition condition;
	public final RALExprUR onTrue;
	public final RALExprUR onFalse;
	public RALTernary(RALCondition c, RALExprUR l, RALExprUR r) {
		condition = c;
		onTrue = l;
		onFalse = r;
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		return new Resolved(condition, onTrue.resolve(context), onFalse.resolve(context));
	}

	public static class Resolved implements RALExpr {
		public final RALCondition condition;
		public final RALExpr onTrue;
		public final RALExpr onFalse;

		public Resolved(RALCondition c, RALExpr t, RALExpr f) {
			condition = c;
			onTrue = t;
			onFalse = f;
		}

		@Override
		public RALType[] outTypes() {
			throw new RuntimeException("Ternary guts NYI (union???)");
		}

		@Override
		public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
			throw new RuntimeException("Ternary guts NYI (union???)");
		}

		@Override
		public RALType inType() {
			throw new RuntimeException("Can't write to ternary");
		}

		@Override
		public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
			throw new RuntimeException("Can't write to ternary");
		}
	}
}
