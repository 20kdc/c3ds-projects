/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContextNW;
import rals.types.RALType;

/**
 * Floating expression, will be used for macros.
 */
public class RALFloatingExpr extends RALDeferredExpr {
	public final Object handle;

	public RALFloatingExpr(Object h, int len, RALType[] rt, RALType[] wt) {
		super(0, len, rt, wt);
		handle = h;
	}

	@Override
	public RALExprSlice getUnderlying(CompileContextNW cc) {
		return cc.module.floatingExprs.get(handle);
	}
}
