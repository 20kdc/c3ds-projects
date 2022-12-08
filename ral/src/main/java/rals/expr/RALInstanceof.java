/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.cond.RALCondSimple;
import rals.types.Classifier;
import rals.types.RALType;

/**
 * Checks that X is an instance of Y.
 */
public class RALInstanceof implements RALExprUR {
	public final Classifier type;
	public final RALExprUR src;

	public RALInstanceof(Classifier t, RALExprUR s) {
		type = t;
		src = s;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		RALExprSlice val = src.resolve(scope);
		RALType valType = val.assert1ReadType().assertImpCast(scope.world.types.gAgentNullable);
		boolean nullable = scope.world.types.byNullable(valType) == valType;
		RALExprSlice params;
		if (type.family == 0) {
			// trying this on Agent?
			// no reasonable way to fail if not nullable
			if (!nullable)
				return new RALConstant.Int(scope.world.types, 1);
			RALExprSlice nullOp = scope.requireLocal("null").content;
			return RALCondSimple.Resolved.of(scope.world.types, RALCondSimple.Op.NotEqual, val, nullOp);
		} else if (type.genus == 0) {
			params = RALExprSlice.concat(
				new RALConstant.Int(scope.world.types, type.family)
			);
		} else if (type.species == 0) {
			params = RALExprSlice.concat(
				new RALConstant.Int(scope.world.types, type.family),
				new RALConstant.Int(scope.world.types, type.genus)
			);
		} else {
			params = RALExprSlice.concat(
				new RALConstant.Int(scope.world.types, type.family),
				new RALConstant.Int(scope.world.types, type.genus),
				new RALConstant.Int(scope.world.types, type.species)
			);
		}
		// start by assuming it's not targ, and then try to check if it is targ
		RALExprSlice finParams = RALExprSlice.concat(
			val,
			params
		);
		String macroName = nullable ? "__ral_compiler_helper_instanceof_nullable" : "__ral_compiler_helper_instanceof";
		RALExprSlice tunnellingCast = val;
		if (tunnellingCast instanceof RALCast.Resolved)
			tunnellingCast = ((RALCast.Resolved) val).expr;
		if (tunnellingCast instanceof RALVarTarg) {
			macroName += "_targ";
			finParams = params;
		}

		return RALCall.makeResolved(macroName, finParams, scope);
	}
}
