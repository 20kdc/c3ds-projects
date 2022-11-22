/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

import java.io.File;

/**
 * Details about a source file.
 */
public class SrcPosFile {
	/**
	 * File that included this source file.
	 */
	public final SrcPos includedFrom;
	/**
	 * Absolute file. This is expected to AT LEAST compare properly.
	 * Some Parser functions treat this as real.
	 */
	public final File absoluteFile;
	/**
	 * Short name for user display.
	 */
	public final String shortName;

	public SrcPosFile(SrcPos f, File a, String s) {
		includedFrom = f;
		absoluteFile = a;
		shortName = s;
	}

	@Override
	public String toString() {
		return shortName;
	}
}
