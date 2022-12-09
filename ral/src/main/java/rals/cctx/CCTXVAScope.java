/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cctx;

import java.util.HashMap;

import rals.code.ScopedVAAllocator;

/**
 * VA Scope component
 */
public class CCTXVAScope {
	public final ScopedVAAllocator alloc;
	public final HashMap<IVAHandle, Integer> heldVAHandles = new HashMap<>();

	public CCTXVAScope() {
		// create the VA allocator
		alloc = new ScopedVAAllocator(new LinearVAAllocator());
		alloc.ensureFree(100);
	}

	public CCTXVAScope(CCTXVAScope sc) {
		alloc = new ScopedVAAllocator(sc.alloc);
		// inherit handles
		heldVAHandles.putAll(sc.heldVAHandles);
	}

	public void close() {
		alloc.close();
	}
}
