/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cctx;

import rals.code.Scripts;
import rals.diag.DiagRecorder;
import rals.types.TypeSystem;

/**
 * Base stuff for compile contexts
 * This is all the stuff we NEVER want to override, plus AutoCloseable stuff
 */
public class CompileContextBase implements AutoCloseable {
	public final TypeSystem typeSystem;
	public final Scripts module;
	public final DiagRecorder diags;

	/**
	 * Just to be sure...
	 */
	private int subUsers = 0;
	private final CompileContextBase subUserTrackingParent;

	public CompileContextBase(TypeSystem ts, Scripts m, DiagRecorder d) {
		typeSystem = ts;
		module = m;
		diags = d;
		// track subusers
		subUserTrackingParent = null;
	}

	public CompileContextBase(CompileContextBase base) {
		typeSystem = base.typeSystem;
		module = base.module;
		diags = base.diags;
		// track subusers
		subUserTrackingParent = base;
		base.subUsers++;
	}

	@Override
	public void close() {
		if (subUsers != 0)
			throw new RuntimeException("Closing a CCTXVAScope with sub-users");
		if (subUserTrackingParent != null)
			subUserTrackingParent.subUsers--;
	}
}
