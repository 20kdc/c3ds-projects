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

	/**
	 * Gets a list of all gene types.
	 */
	public final int[] getGeneTypes() {
		int total = 0;
		for (int v : subTypes)
			total += v;
		int[] res = new int[total];
		int ptr = 0;
		for (int type = 0; type < subTypes.length; type++)
			for (int i = 0; i < subTypes[type]; i++)
				res[ptr++] = i | (type << 8);
		return res;
	}

	/**
	 * Is this the header (i.e. Genus) gene?
	 */
	public final boolean isHeaderGene(int type) {
		return type == 0x0201;
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
	public static final GenVersion C2 = new GenVersion("C2", new int[] {2, 5, 8, 1}, 11) {
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
	public static final GenVersion C3 = new GenVersion("C3/DS/CV/SM", new int[] {3, 6, 9, 1}, 12) {
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

	public static final GenVersion[] VERSIONS = new GenVersion[] {C1, C2, C3};

	static {
		// subType 0

		C1.attach(GenDataLayout.C12__LOBE);
		C2.attach(GenDataLayout.C12__LOBE);
		C3.attach(GenDataLayout.C__3_LOBE);

		C2.attach(GenDataLayout.C_23_BORG);
		C3.attach(GenDataLayout.C_23_BORG);

		C3.attach(GenDataLayout.C__3_TRCT);

		// subType 1

		C1.attach(GenDataLayout.C123_RCPT);
		C2.attach(GenDataLayout.C123_RCPT);
		C3.attach(GenDataLayout.C123_RCPT);

		C1.attach(GenDataLayout.C123_EMIT);
		C2.attach(GenDataLayout.C123_EMIT);
		C3.attach(GenDataLayout.C123_EMIT);

		C1.attach(GenDataLayout.C123_REAC);
		C2.attach(GenDataLayout.C123_REAC);
		C3.attach(GenDataLayout.C123_REAC);

		C1.attach(GenDataLayout.C123_BCHL);
		C2.attach(GenDataLayout.C123_BCHL);
		C3.attach(GenDataLayout.C123_BCHL);

		C1.attach(GenDataLayout.C123_INIT);
		C2.attach(GenDataLayout.C123_INIT);
		C3.attach(GenDataLayout.C123_INIT);

		C3.attach(GenDataLayout.C__3_NEMT);

		// subType 2

		C1.attach(GenDataLayout.C123_STIM);
		C2.attach(GenDataLayout.C123_STIM);
		C3.attach(GenDataLayout.C123_STIM);

		C1.attach(GenDataLayout.C12__GENS);
		C2.attach(GenDataLayout.C12__GENS);
		C3.attach(GenDataLayout.C__3_GENS);

		C1.attach(GenDataLayout.C1___APPR);
		C2.attach(GenDataLayout.C_23_APPR);
		C3.attach(GenDataLayout.C_23_APPR);

		C1.attach(GenDataLayout.C12__POSE);
		C2.attach(GenDataLayout.C12__POSE);
		C3.attach(GenDataLayout.C__3_POSE);

		C1.attach(GenDataLayout.C123_GAIT);
		C2.attach(GenDataLayout.C123_GAIT);
		C3.attach(GenDataLayout.C123_GAIT);

		C1.attach(GenDataLayout.C123_INST);
		C2.attach(GenDataLayout.C123_INST);
		C3.attach(GenDataLayout.C123_INST);

		C1.attach(GenDataLayout.C123_PIGM);
		C2.attach(GenDataLayout.C123_PIGM);
		C3.attach(GenDataLayout.C123_PIGM);

		C2.attach(GenDataLayout.C_23_PIGB);
		C3.attach(GenDataLayout.C_23_PIGB);

		C3.attach(GenDataLayout.C__3_EXPR);

		// subType 3

		C2.attach(GenDataLayout.C_23_ORGN);
		C3.attach(GenDataLayout.C_23_ORGN);
	}
}
