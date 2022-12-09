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

	public CompileContext(TypeSystem ts, Scripts m, DiagRecorder d, CodeWriter cw) {
		super(ts, m, d);
		currentDebugSite = null;
		writer = cw;
		diagsExtent = null;
		cw.debug.initializeRootCC(this);
	}

	protected CompileContext(
			CompileContext sc,
			boolean newVA,
			boolean newEH,
			CCTXBreakScope cbs,
			DebugSite newDebugSite,
			SrcRange de
	) {
		super(sc, newVA, newEH, cbs);
		currentDebugSite = newDebugSite != null ? newDebugSite : sc.currentDebugSite;
		writer = sc.writer;
		diagsExtent = de;
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

	public CompileContext forkBreak(String breakLabel, String breakBool) {
		return new CompileContext(this, false, false, new CCTXBreakScope(breakLabel, breakBool), null, null);
	}

	public CompileContext forkVAEHBreak(String breakLabel, String breakBool) {
		return new CompileContext(this, true, true, new CCTXBreakScope(breakLabel, breakBool), null, null);
	}

	public CompileContext forkDebugDiagExtent(DebugSite dbg, SrcRange diagExtent) {
		return new CompileContext(this, false, false, null, dbg, diagExtent);
	}

	@Override
	public void close() {
		if (diagsExtent != null)
			diags.popFrame(diagsExtent);
		if (ownsVAScope)
			vaScope.close();
	}

	@Override
	public CCTXVAScope getVAScope() {
		return vaScope;
	}

	public String allocLabel() {
		return "_RAL_" + writer.labelNumber++;
	}

	public String getBreakLabel() {
		return breakScope.breakLabel;
	}

	public String getBreakBool() {
		return breakScope.breakBool;
	}
}
