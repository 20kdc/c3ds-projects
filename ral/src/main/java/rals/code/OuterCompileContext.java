/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.diag.DiagRecorder;
import rals.types.TypeSystem;

/**
 * This is used for all the stuff Scripts always wants.
 */
public class OuterCompileContext {
	public final StringBuilder out;
	public final TypeSystem typeSystem;
	public final DiagRecorder diags;
	public final boolean debug;
	public OuterCompileContext(StringBuilder ot, TypeSystem ts, DiagRecorder diag, boolean dbg) {
		out = ot;
		typeSystem = ts;
		diags = diag;
		debug = dbg;
	}
}
