/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

/**
 * Gene flags holder.
 */
public class GeneFlags {
	public static final int C123_MUT = 1;
	public static final int C123_DUP = 2;
	public static final int C123_CUT = 4;
	public static final int C123_MALE = 8;
	public static final int C123_FEMALE = 16;
	public static final int C_23_CARRY = 32;

	public static final String summarizeGeneFlags(byte flags) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		if ((flags & GeneFlags.C123_MUT) != 0)
			sb.append("M");
		if ((flags & GeneFlags.C123_DUP) != 0)
			sb.append("D");
		if ((flags & GeneFlags.C123_CUT) != 0)
			sb.append("C");
		if ((flags & GeneFlags.C123_MALE) != 0)
			sb.append("m");
		if ((flags & GeneFlags.C123_FEMALE) != 0)
			sb.append("f");
		if ((flags & GeneFlags.C_23_CARRY) != 0)
			sb.append("I");
		sb.append(']');
		return sb.toString();
	}
}
