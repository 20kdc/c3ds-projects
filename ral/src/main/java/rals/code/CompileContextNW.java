/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import rals.expr.*;
import rals.types.*;

/**
 * Responsible for holding VA handles.
 * Stuff got moved here so that it's possible to avoid leaking the writer to inline-related methods.
 */
public class CompileContextNW implements AutoCloseable, IVAAllocator {
	public final TypeSystem typeSystem;
	public final Scripts module;
	public final HashMap<IVAHandle, Integer> heldVAHandles = new HashMap<>();
	public final HashMap<IEHHandle, RALExprSlice> heldExprHandles = new HashMap<>();
	public final ScopedVAAllocator alloc;

	/**
	 * Just to be sure...
	 */
	private int subUsers = 0;
	private final CompileContextNW subUserTrackingParent; 

	public final AtomicInteger labelAllocator;

	/**
	 * Two break methods.
	 * If Bool is set, then the bool is set to 1 and you go to the label.
	 * If only Label is set, then you just go to the label.
	 */
	public String breakLabel, breakBool;

	protected CompileContextNW(ScriptContext sc) {
		typeSystem = sc.typeSystem;
		module = sc.module;
		labelAllocator = new AtomicInteger();
		subUserTrackingParent = null;
		// create the VA allocator
		alloc = new ScopedVAAllocator(new LinearVAAllocator());
		alloc.ensureFree(100);
	}

	protected CompileContextNW(CompileContextNW sc) {
		typeSystem = sc.typeSystem;
		module = sc.module;
		labelAllocator = sc.labelAllocator;
		alloc = new ScopedVAAllocator(sc.alloc);
		// inherit break label
		breakLabel = sc.breakLabel;
		breakBool = sc.breakBool;
		// inherit handles
		heldVAHandles.putAll(sc.heldVAHandles);
		heldExprHandles.putAll(sc.heldExprHandles);
		// track subusers
		subUserTrackingParent = sc;
		sc.subUsers++;
	}

	public void clearBreak() {
		breakLabel = null;
		breakBool = null;
	}

	public String allocLabel() {
		return "_RAL_" + labelAllocator.getAndIncrement();
	}

	public int allocVA(IVAHandle obj) {
		int res = allocVA();
		heldVAHandles.put(obj, res);
		return res;
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
		if (subUsers != 0)
			throw new RuntimeException("Closing a CompileContext with sub-users");
		if (subUserTrackingParent != null)
			subUserTrackingParent.subUsers--;
		alloc.close();
	}

	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(String pfx, int va) {
		String res = Integer.toString(va);
		if (res.length() == 1)
			return pfx + "0" + res;
		return pfx + res;
	}

	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(int va) {
		return CompileContextNW.vaToString("va", va);
	}
}
