/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genes;

import cdsp.common.data.IOUtils;
import cdsp.common.data.VirtualCatalogue;
import cdsp.common.data.genetics.GenDataLayout;
import cdsp.common.data.genetics.GenUtils;
import cdsp.common.data.genetics.GenVersion;

/**
 * Genetics handling rewrite: Gene container
 */
public final class Gene {
	/**
	 * Gene version.
	 */
	public final GenVersion version;

	/**
	 * Gene ID. This is a marker attribute to track the gene in genetic editing programs.
	 */
	public int id;

	/**
	 * Gene generation. This rolls over every 256 generations.
	 */
	public int generation;

	/**
	 * Gene activation age. (Some genes ignore this.)
	 */
	public int age;

	/**
	 * Gene flags. (Potentially game-dependent.)
	 */
	public int flags;

	/**
	 * Mutation susceptibility.
	 * Notably, while the 'base chance' is 4800, this number is expressed as a fraction of how many parts to remove from that.
	 * Accounting for other factors, basically a mutability of 255 reduces the per-byte mutation chance to as likely as 1 in 18.
	 */
	public int mutability;

	/**
	 * Variant for CV
	 */
	public int variant;

	/**
	 * Gene data & type.
	 */
	public Data data = new DataUnknown();

	public Gene(GenVersion v) {
		version = v;
	}

	/**
	 * Is this the header (i.e. Genus) gene?
	 */
	public final boolean isHeaderGene() {
		return version.isHeaderGene(data.getType());
	}

	/**
	 * Reads this gene from a byte[].
	 * This overwrites the data field (and all the other fields).
	 */
	public final void deserialize(byte[] target, int offset, int length) {
		int geneType = version.getGeneType(target, offset);
		GenDataLayout gdl = version.getGeneLayout(geneType);
		if (gdl == null) {
			data = new DataUnknown();
		} else {
			data = gdl.newData();
		}
		int type = version.getGeneType(target, offset);
		deserializeHeader(target, offset);
		int lenAdj = length - version.geneHeaderLength;
		if (lenAdj < 0)
			lenAdj = 0;
		data.deserializeData(type, target, offset + version.geneHeaderLength, lenAdj);
	}

	protected void deserializeHeader(byte[] target, int offset) {
		id = GenUtils.safeGet(target, offset + 6);
		generation = GenUtils.safeGet(target, offset + 7);
		age = GenUtils.safeGet(target, offset + 8);
		flags = GenUtils.safeGet(target, offset + 9);
		mutability = GenUtils.safeGet(target, offset + 10);
		variant = GenUtils.safeGet(target, offset + 11, 0, 9);
	}

	/**
	 * Writes this gene, including header, into a byte[] for inclusion in a genome.
	 * The amount of bytes written/to be written can be measured with getGeneLength().
	 */
	public final void serialize(byte[] target, int offset) {
		serializeHeader(target, offset);
		data.serializeData(target, offset + version.geneHeaderLength);
	}

	/**
	 * Serializes this gene as a whole to a byte array.
	 */
	public final byte[] serializeToByteArray() {
		byte[] array = new byte[getGeneLength()];
		serialize(array, 0);
		return array;
	}

	/**
	 * Writes out this gene's header.
	 * This is overridden for C2/C3 to add additional fields.
	 */
	public void serializeHeader(byte[] target, int offset) {
		target[offset++] = (byte) 'g';
		target[offset++] = (byte) 'e';
		target[offset++] = (byte) 'n';
		target[offset++] = (byte) 'e';
		int type = data.getType();
		target[offset++] = (byte) (type >> 8);
		target[offset++] = (byte) type;
		target[offset++] = (byte) id;
		target[offset++] = (byte) generation;
		target[offset++] = (byte) age;
		target[offset++] = (byte) flags;
		if (version.geneHeaderLength >= 11)
			target[offset++] = (byte) mutability;
		if (version.geneHeaderLength >= 12)
			target[offset++] = (byte) variant;
	}

	/**
	 * Summarizes this gene as a whole.
	 */
	public final void summarize(VirtualCatalogue catalogue, StringBuilder builder) {
		summarizeHeader(builder);
		builder.append("\n ");
		data.summarizeData(catalogue, builder);
	}

	/**
	 * Summarizes the header of this gene.
	 */
	public final void summarizeHeader(StringBuilder builder) {
		byte[] headerData = new byte[version.geneHeaderLength];
		serializeHeader(headerData, 0);
		builder.append(version.summarizeGeneHeader(headerData, 0));
	}

	/**
	 * How big is the gene, with header?
	 */
	public final int getGeneLength() {
		return version.geneHeaderLength + data.getDataLength();
	}

	/**
	 * Data inside a gene.
	 */
	public static interface Data {
		/**
		 * Returns gene type as if a big-endian 16-bit unsigned integer.
		 */
		int getType();

		/**
		 * Summarizes the data in this gene.
		 */
		void summarizeData(VirtualCatalogue catalogue, StringBuilder builder);

		/**
		 * Writes out this gene's data.
		 */
		void serializeData(byte[] target, int offset);

		/**
		 * Deserializes this gene's data.
		 */
		void deserializeData(int type, byte[] target, int offset, int length);

		/**
		 * How much data does this gene contain?
		 */
		int getDataLength();
	}

	/**
	 * Unknown data.
	 */
	public static class DataUnknown implements Data {
		public int type;
		public byte[] data;

		public DataUnknown() {
			data = new byte[0];
		}

		@Override
		public int getType() {
			return type;
		}

		@Override
		public void deserializeData(int type, byte[] target, int offset, int length) {
			this.type = type;
			data = new byte[length];
			if (length != 0)
				System.arraycopy(target, offset, data, 0, length);
		}

		@Override
		public int getDataLength() {
			return data.length;
		}

		@Override
		public void serializeData(byte[] target, int offset) {
			System.arraycopy(data, 0, target, offset, data.length);
		}

		@Override
		public void summarizeData(VirtualCatalogue catalogue, StringBuilder builder) {
			builder.append(IOUtils.toHex(data, 0, data.length));
		}
	}
}
