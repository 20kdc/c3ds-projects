/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

import cdsp.common.data.CreaturesFacts;
import cdsp.common.data.genetics.GenDataLayout;
import cdsp.common.data.genetics.GenPackage;
import cdsp.common.data.genetics.GenUtils;
import cdsp.common.data.genetics.GenVersion;
import cdsp.common.s16.Tint;

/**
 * Oh dear goodness.
 */
public class C3SkeletalAgingSimulation {
	public final GenVersion version;
	public final byte[] genome;

	public C3PartSlots myPartSlots = new C3PartSlots(0, 0);
	public Tint myTint = new Tint();

	public C3SkeletalAgingSimulation(GenPackage genome) {
		this.version = genome.version;
		this.genome = genome.data;
	}

	public void executeAge(int age, int sxs) {
		boolean unlockAppr = false;
		int tintR = 0;
		int tintRC = 0;
		int tintG = 0;
		int tintGC = 0;
		int tintB = 0;
		int tintBC = 0;
		int tintRot = 128;
		int tintSwap = 128;
		int offset = 0;
		while (offset < genome.length) {
			offset = GenUtils.nextGene(genome, offset);
			if (offset == genome.length)
				break;
			int geneType = version.getGeneType(genome, offset);
			int geneAge = version.getGeneAge(genome, offset);
			int geneFlags = version.getGeneFlags(genome, offset);
			offset += version.geneHeaderLength;
			if (!version.flagsWillExpress(geneFlags, sxs))
				continue;
			if (geneAge != age)
				continue;
			GenDataLayout gdl = version.getGeneLayout(geneType);
			if (gdl != null) {
				if (gdl == GenDataLayout.C__3_GENS || gdl == GenDataLayout.C12__GENS) {
					int gen = GenUtils.safeGet(genome, offset, 0, CreaturesFacts.GENUS.length);
					myPartSlots = new C3PartSlots(gen, 0);
					unlockAppr = true;
				} else if (unlockAppr && gdl == GenDataLayout.C_23_APPR) {
					int skg = GenUtils.safeGet(genome, offset, 0, SkeletonDef.C3.geneCount);
					int brd = GenUtils.safeGet(genome, offset + 1, 0, CreaturesFacts.C_23_BREED_INDEX.length);
					int gen = GenUtils.safeGet(genome, offset + 2, 0, CreaturesFacts.GENUS.length);
					myPartSlots = myPartSlots.with(skg, gen, brd);
				} else if (gdl == GenDataLayout.C123_PIGM) {
					int ch = GenUtils.safeGet(genome, offset, 0, 3);
					int val = GenUtils.safeGet(genome, offset + 1);
					if (ch == 0) {
						tintR += val;
						tintRC++;
					} else if (ch == 1) {
						tintG += val;
						tintGC++;
					} else if (ch == 2) {
						tintB += val;
						tintBC++;
					}
				} else if (gdl == GenDataLayout.C_23_PIGB) {
					tintRot += GenUtils.safeGet(genome, offset);
					tintSwap += GenUtils.safeGet(genome, offset + 1);
					tintRot >>= 1;
					tintSwap >>= 1;
				}
			}
		}
		if (tintRC == 0) {
			tintR = 128;
			tintRC++;
		}
		if (tintGC == 0) {
			tintG = 128;
			tintGC++;
		}
		if (tintBC == 0) {
			tintB = 128;
			tintBC++;
		}
		myTint = new Tint(tintR / tintRC, tintG / tintGC, tintB / tintBC, tintRot, tintSwap);
	}
}
