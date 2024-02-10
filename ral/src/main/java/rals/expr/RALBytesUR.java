/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.Set;

import rals.code.ScopeContext;
import rals.types.TypeSystem;

/**
 * Unresolved bytes.
 * This only even exists so that it's possible to do constant maths on bytes...
 */
public class RALBytesUR implements RALExprUR {
	public final RALExprUR content;
	public RALBytesUR(RALExprUR c) {
		content = c;
	}

	/**
	 * BIG SCARY NOTE:
	 * This is pretty much the only resolveConst method allowed to directly throw.
	 * Why? Because this expression is only valid as a constant.
	 */
	@Override
	public RALConstant resolveConst(TypeSystem ts, Set<String> scopedVariables) {
		RALExprUR[] details = content.decomposite();
		byte[] target = new byte[details.length];
		for (int i = 0; i < details.length; i++) {
			RALConstant rc = details[i].resolveConst(ts, scopedVariables);
			if (!(rc instanceof RALConstant.Int))
				throw new RuntimeException("Byte " + i + " of byte string not a constant integer");
			RALConstant.Int rci = (RALConstant.Int) rc;
			// if we don't enforce it CAOS will
			if (rci.value < 0 || rci.value > 255)
				throw new RuntimeException("Byte " + i + " of byte string outside of 0-255 range");
			target[i] = (byte) rci.value;
		}
		return new RALConstant.Bytes(ts, target);
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		RALConstant res = resolveConst(scope.world.types, scope.scopedVariables.keySet());
		// we don't need to support non-constant byte strings, because they can't exist
		if (res == null)
			throw new RuntimeException("Byte strings must eventually resolve to constants");
		return res;
	}
}
