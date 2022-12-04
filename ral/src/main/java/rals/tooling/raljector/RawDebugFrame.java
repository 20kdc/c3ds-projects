/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import rals.types.Classifier;
import rals.types.ScriptIdentifier;

/**
 * Raw debug frame information out of dbg#
 */
public class RawDebugFrame {
	public static final String[] INTRINSIC_NAMES = {
		"inst",
		"lock",
		"targ",
		"ownr",
		"from",
		"_it_",
		"part",
		"_p1_",
		"_p2_"
	};
	public final String[] intrinsics = new String[9];
	public final String[] va = new String[100];
	public final ScriptIdentifier inScript;
	public final String caos;
	public final int caosOffset;

	public RawDebugFrame(String[] base, int bIdx, int codf, int codg, int cods, int code, int codp, String src) {
		// copy intrinsics in reverse
		for (int i = 0; i < intrinsics.length; i++)
			intrinsics[8 - i] = base[bIdx + i];
		bIdx += intrinsics.length;
		System.arraycopy(base, bIdx, va, 0, va.length);
		inScript = new ScriptIdentifier(new Classifier(codf, codg, cods), code);
		caos = src;
		caosOffset = codp;
	}
}
