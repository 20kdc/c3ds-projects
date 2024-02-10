/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;

/**
 * Calls a macro, or something like that.
 */
public class RALCall implements RALExprUR {
	public final RALExprUR base;
	public final RALExprUR params;

	public RALCall(String name, RALExprUR p) {
		base = new RALAmbiguousID(null, name);
		params = p;
	}

	public RALCall(RALExprUR b, RALExprUR p) {
		base = b;
		params = p;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		RALExprSlice baseR = base.resolve(context);
		RALExprSlice paramR = params.resolve(context);
		try {
			baseR.assert1ReadType().implicitlyCastOrThrow(context.world.types.gLambdaAny, base, "for use in call");
		} catch (Exception ex) {
			throw new RuntimeException("Type error trying to call " + base, ex);
		}
		return makeResolved(baseR, paramR, context);
	}

	public static RALExprSlice makeResolved(String name, RALExprSlice paramR, ScopeContext context) {
		return makeResolved(new RALAmbiguousID(null, name).resolve(context), paramR, context);
	}
	public static RALExprSlice makeResolved(RALExprSlice base, RALExprSlice paramR, ScopeContext context) {
		RALCallable rc = base.getCallable(0);
		if (rc == null)
			throw new RuntimeException("Unable to actually get RALCallable! Could be out of scope: " + base);
		return rc.instance(paramR, context);
	}
}
