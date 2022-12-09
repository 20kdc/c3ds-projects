/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cctx;

import rals.code.Scripts;
import rals.debug.DebugSite;
import rals.diag.DiagRecorder;
import rals.diag.SrcRange;
import rals.types.TypeSystem;

/**
 * CompileContext is the context for RAL's codegen.
 * CompileContext works based on a single stack but with individually forkable components.
 * All possible forks are controlled from here for consistency reasons.
 */
public class CompileContext extends CompileContextNW implements AutoCloseable, CCTXMixin {
	public final CodeWriter writer;
	public final DebugSite currentDebugSite;
	private final SrcRange diagsExtent;
	public final CCTXLabelScope labelScope;

	public CompileContext(TypeSystem ts, Scripts m, DiagRecorder d, CodeWriter cw) {
		super(ts, m, d);
		currentDebugSite = null;
		writer = cw;
		diagsExtent = null;
		labelScope = new CCTXLabelScope();
		cw.debug.initializeRootCC(this);
	}

	protected CompileContext(
			CompileContext sc,
			boolean newVA,
			boolean newEH,
			DebugSite newDebugSite,
			SrcRange de,
			IBreakHandler escape
	) {
		super(sc, newVA, newEH);
		currentDebugSite = newDebugSite != null ? newDebugSite : sc.currentDebugSite;
		writer = sc.writer;
		diagsExtent = de;
		labelScope = escape != null ? new CCTXLabelScope(sc.labelScope, escape) : sc.labelScope;
		if (de != null)
			diags.pushFrame(de);
	}

	/**
	 * Forks the VA and EH scopes.
	 */
	public CompileContext forkVAEH() {
		return new CompileContext(this, true, true, null, null, null);
	}

	/**
	 * Forks the EH scope only (this is used when something is expected to pass VAs to an exterior scope)
	 */
	@Override
	public CompileContext forkEH() {
		return new CompileContext(this, false, true, null, null, null);
	}

	public CompileContext forkBreak(IBreakHandler escape) {
		assert escape != null;
		return new CompileContext(this, false, false, null, null, escape);
	}

	public CompileContext forkVAEHBreak(IBreakHandler escape) {
		assert escape != null;
		return new CompileContext(this, true, true, null, null, escape);
	}

	public CompileContext forkDebugDiagExtent(DebugSite dbg, SrcRange diagExtent) {
		// Note that dbg can be null if debug info is off, this is expected!
		return new CompileContext(this, false, false, dbg, diagExtent, null);
	}

	@Override
	public void close() {
		if (diagsExtent != null)
			diags.popFrame(diagsExtent);
		if (ownsVAScope)
			vaScope.close();
	}

	@Override
	public CodeWriter internalCodeWriter() {
		return writer;
	}
	@Override
	public CCTXLabelScope internalLabelScope() {
		return labelScope;
	}
	@Override
	public CCTXVAScope internalVAScope() {
		return vaScope;
	}
}
