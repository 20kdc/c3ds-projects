/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cctx;

import java.util.HashMap;

import rals.code.Scripts;
import rals.diag.DiagRecorder;
import rals.expr.*;
import rals.types.*;

/**
 * Responsible for holding VA handles.
 * Stuff got moved here so that it's possible to avoid leaking the writer to inline-related methods.
 */
public abstract class CompileContextNW extends CompileContextBase {
	public final HashMap<IEHHandle, RALExprSlice> heldExprHandles;

	// protected because NW variant isn't supposed to expose this
	protected final CCTXVAScope vaScope;

	protected final boolean ownsVAScope;

	protected CompileContextNW(TypeSystem ts, Scripts m, DiagRecorder d) {
		super(ts, m, d);
		vaScope = new CCTXVAScope();
		ownsVAScope = true;
		heldExprHandles = new HashMap<>();
	}

	protected CompileContextNW(CompileContextNW base, boolean newVA, boolean newEH) {
		super(base);
		// inherit/grab
		vaScope = newVA ? new CCTXVAScope(base.vaScope) : base.vaScope;
		heldExprHandles = newEH ? new HashMap<>(base.heldExprHandles) : base.heldExprHandles;
		ownsVAScope = newVA;
	}

	public abstract CompileContextNW forkEH();

	/**
	 * Looks up a VA.
	 * This is needed even if in a CompileContextNW.
	 * This is because CompileContextNW's point is for stuff like inline statements.
	 */
	public Integer lookupVA(IVAHandle handle) {
		return vaScope.heldVAHandles.get(handle);
	}
}
