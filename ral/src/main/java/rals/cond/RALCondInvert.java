/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.expr.RALConstant;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.types.TypeSystem;

/**
 * Boolean NOT
 */
public class RALCondInvert implements RALExprUR {
	public final RALExprUR inside;
	public RALCondInvert(RALExprUR i) {
		inside = i;
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant rc = inside.resolveConst(ts);
		if (rc == null)
			return null;
		return RALCondition.boolToConst(ts, !RALCondition.constToBool(rc));
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		RALCondition resolved = RALCondition.coerceToCondition(inside.resolve(scope), scope.script.typeSystem);
		return new RALCondition(scope.script.typeSystem) {
			@Override
			public String compileCond(StringBuilder writer, CompileContext sharedContext, boolean invert) {
				return resolved.compileCond(writer, sharedContext, !invert);
			}
		};
	}

}
