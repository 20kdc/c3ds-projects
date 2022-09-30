/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.expr.RALStringVar;
import rals.types.RALType;

/**
 * Allocates variable slots.
 */
public interface IVAAllocator {
	/**
	 * Allocates a given VA.
	 */
	int allocVA();

	/**
	 * Releases a given VA.
	 */
	void releaseVA(int i);

	/**
	 * Allocates a VA and returns it as a RALStringVar.
	 */
	default RALStringVar allocVA(RALType t) {
		int slot = allocVA();
		String slotS = ScopeContext.vaToString(slot);
		RALStringVar res = new RALStringVar(slotS, t, true);
		return res;
	}
}
