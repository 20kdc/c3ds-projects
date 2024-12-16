/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

import cdsp.common.data.genes.Gene;

/**
 * Genome versions.
 */
public abstract class GenVersion {
	public final String name;
	public final int geneHeaderLength;
	private final GenDataLayout[] geneTypes = new GenDataLayout[0x10000];
	private final int[] subTypes;

	public GenVersion(String whatami, int[] st, int hdr) {
		name = whatami;
		subTypes = st;
		geneHeaderLength = hdr;
	}

	@Override
	public String toString() {
		return name;
	}

	private void attach(GenDataLayout gdl) {
		geneTypes[gdl.type] = gdl;
	}

	public final int getGeneType(byte[] genome, int start) {
		int high = GenUtils.safeGet(genome, start + 4, 0, subTypes.length);
		int low = GenUtils.safeGet(genome, start + 5, 0, subTypes[high]);
		return (high << 8) | low;
	}

	public final int getGeneAge(byte[] genome, int start) {
		return GenUtils.safeGet(genome, start + 8);
	}

	/**
	 * See GF_* constants
	 */
	public final int getGeneFlags(byte[] genome, int start) {
		return GenUtils.safeGet(genome, start + 9);
	}

	/**
	 * If a gene will express (based on flags).
	 */
	public abstract boolean flagsWillExpress(int geneFlags, int sxs);

	/**
	 * Gets the known data layout of a gene by type.
	 */
	public final GenDataLayout getGeneLayout(int geneType) {
		return geneTypes[geneType];
	}

	/**
	 * Names a gene type with name & raw name
	 */
	public final String describeGeneType(int geneTypeV) {
		String geneType = Integer.toHexString(geneTypeV);
		while (geneType.length() < 4)
			geneType = "0" + geneType;
		GenDataLayout gdl = getGeneLayout(geneTypeV);
		if (gdl != null)
			geneType += " (" + gdl.name + ")";
		return geneType;
	}

	/**
	 * Summarizes a gene header.
	 */
	public abstract String summarizeGeneHeader(byte[] genome, int start);

	/**
	 * Summarizes a gene.
	 */
	public final String summarizeGene(byte[] genome, int start) {
		StringBuilder sb = new StringBuilder();
		Gene g = new Gene(this);
		int len = GenUtils.nextChunk(genome, start + geneHeaderLength) - start;
		if (len < 0)
			len = 0;
		g.deserialize(genome, start, len);
		g.summarize(sb);
		return sb.toString();
	}

	public static final GenVersion C1 = new GenVersion("C1", new int[] {1, 5, 7}, 10) {
		public boolean flagsWillExpress(int geneFlags, int sxs) {
			if ((geneFlags & (GeneFlags.C123_MALE | GeneFlags.C123_FEMALE)) != 0) {
				int relevantFlag = sxs == 0 ? GeneFlags.C123_MALE : GeneFlags.C123_FEMALE;
				if ((geneFlags & relevantFlag) == 0)
					return false;
			}
			return true;
		}
		@Override
		public String summarizeGeneHeader(byte[] genome, int start) {
			int geneTypeV = getGeneType(genome, start);
			String geneType = describeGeneType(geneTypeV);
			int geneId = GenUtils.safeGet(genome, start + 6);
			int generation = GenUtils.safeGet(genome, start + 7);
			int switchOn = GenUtils.safeGet(genome, start + 8);
			int flags = GenUtils.safeGet(genome, start + 9);
			return geneType + " I" + (geneId & 0xFF) + " G" + (generation & 0xFF) + " A" + (switchOn & 0xFF) + " " + GeneFlags.summarizeGeneFlags((byte) flags);
		}
	};
	public static final GenVersion C2 = new GenVersion("C2", new int[] {2, 5, 1, 8}, 11) {
		public boolean flagsWillExpress(int geneFlags, int sxs) {
			if ((geneFlags & GeneFlags.C_23_CARRY) != 0)
				return false;
			if ((geneFlags & (GeneFlags.C123_MALE | GeneFlags.C123_FEMALE)) != 0) {
				int relevantFlag = sxs == 0 ? GeneFlags.C123_MALE : GeneFlags.C123_FEMALE;
				if ((geneFlags & relevantFlag) == 0)
					return false;
			}
			return true;
		}

		@Override
		public String summarizeGeneHeader(byte[] genome, int start) {
			int geneTypeV = getGeneType(genome, start);
			String geneType = describeGeneType(geneTypeV);
			int geneId = GenUtils.safeGet(genome, start + 6);
			int generation = GenUtils.safeGet(genome, start + 7);
			int switchOn = GenUtils.safeGet(genome, start + 8);
			int flags = GenUtils.safeGet(genome, start + 9);
			int mutability = GenUtils.safeGet(genome, start + 10);
			return geneType + " I" + (geneId & 0xFF) + " G" + (generation & 0xFF) + " A" + (switchOn & 0xFF) + " " + GeneFlags.summarizeGeneFlags((byte) flags) + " M" + (mutability & 0xFF);
		}
	};
	public static final GenVersion C3 = new GenVersion("C3/DS/CV/SM", new int[] {3, 6, 1, 9}, 12) {
		public boolean flagsWillExpress(int geneFlags, int sxs) {
			if ((geneFlags & GeneFlags.C_23_CARRY) != 0)
				return false;
			if ((geneFlags & (GeneFlags.C123_MALE | GeneFlags.C123_FEMALE)) != 0) {
				int relevantFlag = sxs == 0 ? GeneFlags.C123_MALE : GeneFlags.C123_FEMALE;
				if ((geneFlags & relevantFlag) == 0)
					return false;
			}
			return true;
		}

		@Override
		public String summarizeGeneHeader(byte[] genome, int start) {
			int geneTypeV = getGeneType(genome, start);
			String geneType = describeGeneType(geneTypeV);
			int geneId = GenUtils.safeGet(genome, start + 6);
			int generation = GenUtils.safeGet(genome, start + 7);
			int switchOn = GenUtils.safeGet(genome, start + 8);
			int flags = GenUtils.safeGet(genome, start + 9);
			int mutability = GenUtils.safeGet(genome, start + 10);
			int variant = GenUtils.safeGet(genome, start + 11);
			return geneType + " I" + (geneId & 0xFF) + " G" + (generation & 0xFF) + " A" + (switchOn & 0xFF) + " " + GeneFlags.summarizeGeneFlags((byte) flags) + " M" + (mutability & 0xFF) + " V" + (variant & 0xFF);
		}
	};

	static {
		C3.attach(GenDataLayout.C__3_LOBE);
		C3.attach(GenDataLayout.C__3_BORG);
		C3.attach(GenDataLayout.C__3_GENS);

		C1.attach(GenDataLayout.C123_BCHL);
		C2.attach(GenDataLayout.C123_BCHL);
		C3.attach(GenDataLayout.C123_BCHL);

		C2.attach(GenDataLayout.C_23_APPR);
		C3.attach(GenDataLayout.C_23_APPR);

		C3.attach(GenDataLayout.C__3_POSE);

		C1.attach(GenDataLayout.C123_GAIT);
		C2.attach(GenDataLayout.C123_GAIT);
		C3.attach(GenDataLayout.C123_GAIT);

		C1.attach(GenDataLayout.C123_PIGM);
		C2.attach(GenDataLayout.C123_PIGM);
		C3.attach(GenDataLayout.C123_PIGM);

		C3.attach(GenDataLayout.C__3_PIGB);
	}
}
