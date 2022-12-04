/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.debug.DebugSite;
import rals.diag.DiagRecorder;
import rals.diag.SrcPos;
import rals.types.TypeSystem;
import rals.types.RALType;
import rals.expr.*;

/**
 * Responsible for holding VA handles.
 */
public class CompileContext extends CompileContextNW implements AutoCloseable, IVAAllocator {
	public final CodeWriter writer;

	public final ScopedVAAllocator alloc;

	/**
	 * Just to be sure...
	 */
	private int subUsers = 0;
	private final CompileContext subUserTrackingParent;

	/**
	 * BEWARE: CAN BE NULL (especially if shouldGenerateSites is false)
	 * Also, this is managed on-stack.
	 */
	public DebugSite currentDebugSite;

	public CompileContext(TypeSystem ts, Scripts m, DiagRecorder d, CodeWriter cw) {
		super(ts, m, d);
		currentDebugSite = null;
		writer = cw;
		// create the VA allocator
		alloc = new ScopedVAAllocator(new LinearVAAllocator());
		alloc.ensureFree(100);
		cw.debug.initializeRootCC(this);
		// track subusers
		subUserTrackingParent = null;
	}

	public CompileContext(CompileContext sc) {
		super(sc);
		currentDebugSite = sc.currentDebugSite;
		writer = sc.writer;
		alloc = new ScopedVAAllocator(sc.alloc);
		// track subusers
		subUserTrackingParent = sc;
		sc.subUsers++;
	}

	@Override
	public int allocVA() {
		return alloc.allocVA();
	}

	@Override
	public void allocVA(int i) {
		alloc.allocVA(i);
	}

	/**
	 * Allocates a VA and assigns it to the given VA handle.
	 */
	public int allocVA(IVAHandle obj) {
		int res = allocVA();
		heldVAHandles.put(obj, res);
		return res;
	}

	/**
	 * Allocates the given VA and assigns it to the given VA handle.
	 */
	public void allocVA(IVAHandle obj, int i) {
		allocVA(i);
		heldVAHandles.put(obj, i);
	}

	/**
	 * Allocates a VA and returns it as a RALVarVA.
	 */
	public RALVarVA allocVA(final RALType t, final String site) {
		IVAHandle handle = new IVAHandle() {
			@Override
			public String toString() {
				return "allocVA:" + t + ":" + site;
			}
		};
		allocVA(handle);
		return new RALVarVA(handle, t);
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
}
