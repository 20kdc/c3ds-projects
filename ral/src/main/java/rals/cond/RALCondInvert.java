/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.cctx.*;
import rals.code.*;
import rals.expr.*;
import rals.types.*;

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
	public RALExprSlice resolveInner(ScopeContext scope) {
		RALCondition resolved = RALCondition.coerceToCondition(inside.resolve(scope), scope.world.types);
		return new RALCondition(scope.world.types) {
			@Override
			public String compileCond(CodeWriter writer, CompileContext sharedContext, boolean invert) {
				return resolved.compileCond(writer, sharedContext, !invert);
			}
		};
	}

}
