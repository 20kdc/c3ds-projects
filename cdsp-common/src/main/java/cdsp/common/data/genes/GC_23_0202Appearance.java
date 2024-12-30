/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genes;

import cdsp.common.data.CreaturesFacts;
import cdsp.common.data.VirtualCatalogue;
import cdsp.common.data.genetics.GenUtils;
import cdsp.common.data.skeleton.SkeletonDef;

/**
 * Appearance gene for Creatures 3.
 */
public class GC_23_0202Appearance implements Gene.Data {
	public int geneIndex;
	public int breedIndex;
	public int genus;

	public GC_23_0202Appearance() {
		super();
	}

	@Override
	public int getType() {
		return 0x0202;
	}

	@Override
	public void summarizeData(VirtualCatalogue catalogue, StringBuilder builder) {
		builder.append(catalogue.getSkeletonDef().getGeneName(geneIndex));
		builder.append(" = ");
		builder.append(CreaturesFacts.GENUS[genus]);
		builder.append(' ');
		builder.append(CreaturesFacts.C_23_BREED_INDEX[breedIndex].toUpperCase());
	}

	@Override
	public int getDataLength() {
		return 3;
	}

	@Override
	public void deserializeData(int type, byte[] target, int offset, int length) {
		geneIndex = GenUtils.safeGet(target, offset++, 0, SkeletonDef.C3.geneCount);
		breedIndex = GenUtils.safeGet(target, offset++, 0, CreaturesFacts.C_23_BREED_INDEX.length);
		genus = GenUtils.safeGet(target, offset++, 0, CreaturesFacts.GENUS.length);
	}

	@Override
	public void serializeData(byte[] target, int offset) {
		target[offset++] = (byte) geneIndex;
		target[offset++] = (byte) breedIndex;
		target[offset++] = (byte) genus;
	}
}
