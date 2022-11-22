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
 * Responsible for holding VA handles.
 */
public class CompileContext extends CompileContextNW {
	public final CodeWriter writer;

	public CompileContext(TypeSystem ts, Scripts m, DiagRecorder d, CodeWriter cw) {
		super(ts, m, d);
		writer = cw;
	}

	public CompileContext(CompileContext sc) {
		super(sc);
		writer = sc.writer;
	}
}
