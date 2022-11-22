/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

import java.io.File;

/**
 * Position in source code.
 */
public class SrcPos {
	public final SrcPosFile file;
	public final int line;
	public SrcPos(SrcPosFile f, int l) {
		file = f;
		line = l;
	}

	@Override
	public String toString() {
		return file + ":" + line;
	}
}
