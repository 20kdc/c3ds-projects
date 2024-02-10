/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.LinkedList;

import rals.code.*;
import rals.expr.RALSlot.Perm;
import rals.types.RALType;

/**
 * This is likely to change to match macros if it gets expanded into a full lambda mechanism.
 * For now, it simply handles the equivalent to Runnable.
 */
public class RALLambda implements RALExprUR {
	public final MacroArg[] args;
	public final RALExprUR content;

	public RALLambda(MacroArg[] args, RALExprUR st) {
		this.args = args;
		content = st;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		ScopeContext sc = new ScopeContext(scope);
		if (args.length != 0)
			throw new RuntimeException("args in lambda not yet supported");
		final RALExprSlice rStmtExpr = content.resolve(sc);
		LinkedList<RALType> lambdaRets = new LinkedList<>();
		for (int i = 0; i < rStmtExpr.length; i++) {
			RALSlot slot = rStmtExpr.slot(i);
			slot.perms.require(rStmtExpr, Perm.R);
			lambdaRets.add(slot.type);
		}
		RALType lambdaType = scope.world.types.byLambda(lambdaRets);
		return new RALConstant.Callable(lambdaType, new RALCallable() {
			@Override
			public RALExprSlice instance(RALExprSlice args, ScopeContext sc) {
				return rStmtExpr;
			}
		});
	}
}
