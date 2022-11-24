/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.diag.DiagRecorder;
import rals.hcm.IHCMRecorder;
import rals.types.TypeSystem;

/**
 * So there's an awful lot of globals that unresolved stuff cares about, and it all has to go somewhere.
 */
public class UnresolvedWorld {
	public final TypeSystem types;
	public final ScriptsUR module;
	public final DiagRecorder diags;
	public final IHCMRecorder hcm;

	public UnresolvedWorld(TypeSystem ts, ScriptsUR m, DiagRecorder d, IHCMRecorder h) {
		types = ts;
		module = m;
		diags = d;
		hcm = h;
	}
}
