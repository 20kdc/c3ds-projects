/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.data.skeleton;

/**
 * Creatures 3 part slot data
 */
public class C3PartSlots {
	private final int[] genuses = new int[SkeletonDef.C3.geneCount];
	private final int[] breedSlots = new int[SkeletonDef.C3.geneCount];

	public C3PartSlots(int g, int bs) {
		for (int i = 0; i < genuses.length; i++) {
			genuses[i] = g;
			breedSlots[i] = bs;
		}
	}
	public C3PartSlots(int[] g, int[] bs) {
		System.arraycopy(g, 0, genuses, 0, g.length);
		System.arraycopy(bs, 0, breedSlots, 0, bs.length);
	}

	public int getGenus(int gene) {
		return genuses[gene];
	}

	public int getBreedSlot(int gene) {
		return breedSlots[gene];
	}

	public C3SkeletonIndex[] asSkeletonIndexes(int s, int a) {
		C3SkeletonIndex[] indexes = new C3SkeletonIndex[breedSlots.length];
		for (int i = 0; i < indexes.length; i++)
			indexes[i] = new C3SkeletonIndex(breedSlots[i], genuses[i], s, a);
		return indexes;
	}

	public C3PartSlots with(int gene, int genus, int breedSlot) {
		C3PartSlots ns = new C3PartSlots(genuses, breedSlots);
		ns.breedSlots[gene] = breedSlot;
		ns.genuses[gene] = genus;
		return ns;
	}
}
