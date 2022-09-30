/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;

import rals.types.TypeSystem;

/**
 * Responsible for holding VA handles.
 */
public class CompileContext implements AutoCloseable, IVAAllocator {
	public final TypeSystem typeSystem;
	public final Module module;
	public final HashMap<IVAHandle, Integer> heldVAHandles = new HashMap<>();
	public final ScopedVAAllocator alloc;

	public CompileContext(ScriptContext sc) {
		typeSystem = sc.typeSystem;
		module = sc.module;
		// create the VA allocator
		alloc = new ScopedVAAllocator(new LinearVAAllocator());
		alloc.ensureFree(100);
	}

	public CompileContext(CompileContext sc) {
		typeSystem = sc.typeSystem;
		module = sc.module;
		alloc = new ScopedVAAllocator(sc.alloc);
		heldVAHandles.putAll(sc.heldVAHandles);
	}

	public void allocVA(IVAHandle obj) {
		heldVAHandles.put(obj, allocVA());
	}

	@Override
	public int allocVA() {
		return alloc.allocVA();
	}

	@Override
	public void releaseVA(int i) {
		alloc.releaseVA(i);
	}

	@Override
	public void close() {
		alloc.close();
	}
}
