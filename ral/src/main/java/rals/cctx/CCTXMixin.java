/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cctx;

import java.util.Map;
import java.util.Set;

import rals.expr.RALVarVA;
import rals.types.RALType;

/**
 * Used to move code out of the very repetitive CompileContext.
 */
public interface CCTXMixin {
	CCTXVAScope getVAScope();

	/**
	 * Allocates a VA and assigns it to the given VA handle.
	 */
	default int allocVA(IVAHandle obj) {
		CCTXVAScope vac = getVAScope();
		int res = vac.alloc.allocVA();
		vac.heldVAHandles.put(obj, res);
		return res;
	}

	/**
	 * Allocates the given VA and does nothing with it.
	 */
	default void allocVA(int i) {
		CCTXVAScope vac = getVAScope();
		vac.alloc.allocVA(i);
	}

	/**
	 * Entry set
	 */
	default Set<Map.Entry<IVAHandle, Integer>> getVAHandleEntrySet() {
		return getVAScope().heldVAHandles.entrySet();
	}

	/**
	 * Allocates the given VA and assigns it to the given VA handle.
	 */
	default void allocVA(IVAHandle obj, int i) {
		CCTXVAScope vac = getVAScope();
		vac.alloc.allocVA(i);
		vac.heldVAHandles.put(obj, i);
	}

	/**
	 * Allocates a VA and returns it as a RALVarVA.
	 */
	default RALVarVA allocVA(final RALType t, final String site) {
		IVAHandle handle = new IVAHandle() {
			@Override
			public String toString() {
				return "allocVA:" + t + ":" + site;
			}
		};
		allocVA(handle);
		return new RALVarVA(handle, t);
	}

}
