/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.types.RALType;

/**
 * Stringifies non-string values.
 */
public class RALImplicitStringifier implements RALExprUR {
	public final RALExprUR input;
	public RALImplicitStringifier(RALExprUR inp) {
		input = inp;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		RALExprSlice slice = input.resolve(scope);
		RALType rt = slice.assert1ReadType();
		if (rt.canImplicitlyCast(scope.script.world.types.gString))
			return slice;
		if (rt.canImplicitlyCast(scope.script.world.types.gNumber))
			return new RALInlineExpr.Resolved(new RALSlot(scope.world.types.gString, RALSlot.Perm.R), new Object[] {"vtos ", slice});
		throw new RuntimeException("Cannot stringify " + rt);
	}
}
