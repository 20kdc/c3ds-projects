/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.expr.RALExpr;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * Allocates and releases VA slots.
 */
public class ScriptContext {
	public final boolean[] allocMap = new boolean[100];
	public final RALType ownrType;
	public final TypeSystem typeSystem;
	public final Module module;

	public ScriptContext(TypeSystem ts, Module m, RALType ot) {
		typeSystem = ts;
		module = m;
		ownrType = ot;
	}

	public final int allocateVA() {
		for (int i = 0; i < allocMap.length; i++) {
			if (!allocMap[i]) {
				allocMap[i] = true;
				return i;
			}
		}
		throw new RuntimeException("Out of VAs");
	}
	public final void releaseVA(int i) {
		allocMap[i] = false;
	}
}
