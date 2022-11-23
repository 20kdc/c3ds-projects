/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.types.*;

/**
 * Used by the inverted statement expression stuff.
 */
public class RALVarEH extends RALExprSlice.Deferred {
	public final IEHHandle handle;
	public final RALType type;

	public RALVarEH(IEHHandle h, RALType ot) {
		super(0, 1, new RALType[] {ot}, new RALType[] {ot});
		handle = h;
		type = ot;
	}

	@Override
	public String toString() {
		return "EH[" + handle + "!" + type + "]";
	}

	public RALExprSlice getUnderlying(CompileContextNW cc) {
		RALExprSlice ex = cc.heldExprHandles.get(handle);
		if (ex == null)
			throw new RuntimeException("Missing: " + this);
		return ex;
	}
}
