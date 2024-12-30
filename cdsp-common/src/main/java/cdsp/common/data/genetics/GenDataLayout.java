/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

import cdsp.common.data.genes.GC123_0102Reaction;
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
		if (newData().getDataLength() != l)
			throw new RuntimeException("GenDataLayout consistency error");
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

	// subType 0
	public static final GenDataLayout C12__LOBE = new GenDataLayout(0x0000, 112, "Lobe");
	public static final GenDataLayout C__3_LOBE = new GenDataLayout(0x0000, 121, "Lobe");
	public static final GenDataLayout C_23_BORG = new GenDataLayout(0x0001, 5, "Brain Organ");
	public static final GenDataLayout C__3_TRCT = new GenDataLayout(0x0002, 128, "Tract");
	// subType 1
	public static final GenDataLayout C123_RCPT = new GenDataLayout(0x0100, 8, "Receptor");
	public static final GenDataLayout C123_EMIT = new GenDataLayout(0x0101, 8, "Emitter");
	public static final GenDataLayout C123_REAC = new GenDataLayout(0x0102, 9, "Reaction", GC123_0102Reaction.class);
	public static final GenDataLayout C123_BCHL = new GenDataLayout(0x0103, 256, "Half-Lives");
	public static final GenDataLayout C123_INIT = new GenDataLayout(0x0104, 2, "Initial Chemical");
	public static final GenDataLayout C__3_NEMT = new GenDataLayout(0x0105, 15, "Neuroemitter");
	// subType 2
	public static final GenDataLayout C123_STIM = new GenDataLayout(0x0200, 13, "Stimulus");
	public static final GenDataLayout C12__GENS = new GenDataLayout(0x0201, 9, "Genus");
	public static final GenDataLayout C__3_GENS = new GenDataLayout(0x0201, 65, "Genus");
	public static final GenDataLayout C1___APPR = new GenDataLayout(0x0202, 2, "Appearance");
	public static final GenDataLayout C_23_APPR = new GenDataLayout(0x0202, 3, "Appearance", GC_23_0202Appearance.class);
	public static final GenDataLayout C12__POSE = new GenDataLayout(0x0203, 16, "Pose");
	public static final GenDataLayout C__3_POSE = new GenDataLayout(0x0203, 17, "Pose");
	public static final GenDataLayout C123_GAIT = new GenDataLayout(0x0204, 9, "Gait");
	public static final GenDataLayout C123_INST = new GenDataLayout(0x0205, 9, "Instinct");
	public static final GenDataLayout C123_PIGM = new GenDataLayout(0x0206, 2, "Pigment");
	public static final GenDataLayout C_23_PIGB = new GenDataLayout(0x0207, 2, "Pigment Bleed");
	public static final GenDataLayout C__3_EXPR = new GenDataLayout(0x0208, 11, "Expression");
	// subType 3
	public static final GenDataLayout C_23_ORGN = new GenDataLayout(0x0300, 5, "Organ");
}
