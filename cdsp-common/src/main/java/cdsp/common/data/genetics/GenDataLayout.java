/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

import cdsp.common.data.genes.GC_23_0202Appearance;
import cdsp.common.data.genes.Gene;
import cdsp.common.data.genes.Gene.Data;

/**
 * Gene data layouts; aka gene types with version info.
 */
public final class GenDataLayout {
	public final int type;
	public final int length;
	public final String name;
	public final Class<Gene.Data> geneClass;

	public GenDataLayout(int t, int l, String name) {
		type = t;
		length = l;
		this.name = name;
		geneClass = null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Gene.Data> GenDataLayout(int t, int l, String name, Class<T> geneClass) {
		type = t;
		length = l;
		this.name = name;
		this.geneClass = (Class<Gene.Data>) geneClass;
	}

	public Data newData() {
		if (geneClass == null) {
			return new Gene.DataUnknown();
		} else {
			try {
				return geneClass.newInstance();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static final GenDataLayout C__3_LOBE = new GenDataLayout(0x0000, 121, "Lobe");
	public static final GenDataLayout C__3_BORG = new GenDataLayout(0x0001, 20, "Brain Organ");
	// (...)
	public static final GenDataLayout C123_BCHL = new GenDataLayout(0x0103, 256, "Half-Lives");
	// (...)
	public static final GenDataLayout C__3_GENS = new GenDataLayout(0x0201, 65, "Genus");
	public static final GenDataLayout C_23_APPR = new GenDataLayout(0x0202, 3, "Appearance", GC_23_0202Appearance.class);
	public static final GenDataLayout C__3_POSE = new GenDataLayout(0x0203, 17, "Pose");
	public static final GenDataLayout C123_GAIT = new GenDataLayout(0x0204, 9, "Gait");
	// (...)
	public static final GenDataLayout C123_PIGM = new GenDataLayout(0x0206, 2, "Pigment");
	public static final GenDataLayout C__3_PIGB = new GenDataLayout(0x0207, 2, "Pigment Bleed");
}
