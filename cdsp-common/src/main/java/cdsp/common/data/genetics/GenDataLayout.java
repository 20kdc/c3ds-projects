/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

import cdsp.common.data.CreaturesFacts;
import cdsp.common.data.IOUtils;
import cdsp.common.data.skeleton.SkeletonDef;

/**
 * Gene data layouts; aka gene types with version info.
 */
public class GenDataLayout {
	public final int type;
	public final int length;
	public final String name;

	public GenDataLayout(int t, int l, String name) {
		type = t;
		length = l;
		this.name = name;
	}

	/**
	 * Summarizes the gene data.
	 * Be careful to ensure it is the data, not the gene header.
	 */
	public String summarize(byte[] data, int offset) {
		return IOUtils.toHex(data, offset, length);
	}

	public static final GenDataLayout C__3_LOBE = new GenDataLayout(0x0000, 121, "Lobe");
	public static final GenDataLayout C__3_BORG = new GenDataLayout(0x0001, 20, "Brain Organ");
	// (...)
	public static final GenDataLayout C123_BCHL = new GenDataLayout(0x0103, 256, "Half-Lives");
	// (...)
	public static final GenDataLayout C__3_GENS = new GenDataLayout(0x0201, 65, "Genus");
	public static final GenDataLayout C_23_APPR = new GenDataLayout(0x0202, 3, "Appearance") {
		@Override
		public String summarize(byte[] data, int offset) {
			String gene = SkeletonDef.C3.getGeneName(GenUtils.safeGet(data, offset, 0, SkeletonDef.C3.geneCount));
			String breed = CreaturesFacts.C_23_BREED_INDEX[GenUtils.safeGet(data, offset + 1, 0, CreaturesFacts.C_23_BREED_INDEX.length)];
			String genus = CreaturesFacts.GENUS[GenUtils.safeGet(data, offset + 2, 0, CreaturesFacts.GENUS.length)];
			return gene + " = " + genus + " " + breed.toUpperCase();
		}
	};
	public static final GenDataLayout C__3_POSE = new GenDataLayout(0x0203, 17, "Pose");
	public static final GenDataLayout C123_GAIT = new GenDataLayout(0x0204, 9, "Gait");
	// (...)
	public static final GenDataLayout C123_PIGM = new GenDataLayout(0x0206, 2, "Pigment");
	public static final GenDataLayout C__3_PIGB = new GenDataLayout(0x0207, 2, "Pigment Bleed");
}
