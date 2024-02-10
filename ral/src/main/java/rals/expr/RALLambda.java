/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.Arrays;
import java.util.LinkedList;

import rals.code.*;
import rals.diag.SrcRange;
import rals.expr.RALSlot.Perm;
import rals.lex.DefInfo;
import rals.types.RALType;

/**
 * This is likely to change to match macros if it gets expanded into a full lambda mechanism.
 * For now, it simply handles the equivalent to Runnable.
 */
public class RALLambda implements RALExprUR {
	public final SrcRange where;
	public final MacroArg[] args;
	public final RALExprUR content;

	public RALLambda(SrcRange where, MacroArg[] args, RALExprUR st) {
		this.where = where;
		this.args = args;
		content = st;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		ScopeContext sc = new ScopeContext(scope);

		scope.world.diags.pushFrame(where);

		for (MacroArg arg : args)
			sc.setLoc(arg.name, new DefInfo.Builtin(arg.toString()), new RALVarEH(arg, arg.type));

		final RALExprSlice rStmtExpr = content.resolve(sc);

		LinkedList<RALType> lambdaRets = new LinkedList<>();
		for (int i = 0; i < rStmtExpr.length; i++) {
			RALSlot slot = rStmtExpr.slot(i);
			slot.perms.require(rStmtExpr, Perm.R);
			lambdaRets.add(slot.type);
		}
		RALType lambdaType = sc.world.types.byLambda(lambdaRets, Arrays.asList(args));

		scope.world.diags.popFrame(where);

		return new RALConstant.Callable(lambdaType, new RALCallable() {
			@Override
			public RALExprSlice instance(RALExprSlice argsV, ScopeContext sc) {
				String what = "(lambda " + where + ")";
				VarCacher vc = Macro.varCacherFromMacroArgs(what, argsV, args);
				return new Macro.Resolved(what, where, vc, rStmtExpr, args, sc.world.types);
			}
		});
	}
}
