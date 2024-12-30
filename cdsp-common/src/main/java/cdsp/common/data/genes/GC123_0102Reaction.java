/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.data.genes;

import cdsp.common.data.VirtualCatalogue;
import cdsp.common.data.genetics.GenUtils;

/**
 * Reaction
 */
public class GC123_0102Reaction implements Gene.Data {
	public int inAAmount; // 1 to 16 inclusive
	public int inAChem;
	public int inBAmount;
	public int inBChem;
	public int outAAmount;
	public int outAChem;
	public int outBAmount;
	public int outBChem;
	public int rate;

	@Override
	public int getType() {
		return 0x0102;
	}

	@Override
	public void summarizeData(VirtualCatalogue catalogue, StringBuilder builder) {
		builder.append("(");
		if (inAChem != 0) {
			GenUtils.summarizeChemRef(catalogue, builder, inAChem);
			builder.append("x");
			builder.append(inAAmount);
		}
		if (inAChem != 0 && inBChem != 0)
			builder.append(" + ");
		if (inBChem != 0) {
			GenUtils.summarizeChemRef(catalogue, builder, inBChem);
			builder.append("x");
			builder.append(inBAmount);
		}
		builder.append(") -> (");
		if (outAChem != 0) {
			GenUtils.summarizeChemRef(catalogue, builder, outAChem);
			builder.append("x");
			builder.append(outAAmount);
		}
		if (outAChem != 0 && outBChem != 0)
			builder.append(" + ");
		if (outBChem != 0) {
			GenUtils.summarizeChemRef(catalogue, builder, outBChem);
			builder.append("x");
			builder.append(outBAmount);
		}
		builder.append(") @ ");
		builder.append(rate);
	}

	@Override
	public void serializeData(byte[] target, int offset) {
		target[offset++] = (byte) inAAmount;
		target[offset++] = (byte) inAChem;
		target[offset++] = (byte) inBAmount;
		target[offset++] = (byte) inBChem;
		target[offset++] = (byte) outAAmount;
		target[offset++] = (byte) outAChem;
		target[offset++] = (byte) outBAmount;
		target[offset++] = (byte) outBChem;
		target[offset++] = (byte) rate;
	}

	@Override
	public void deserializeData(int type, byte[] target, int offset, int length) {
		inAAmount = GenUtils.safeGet(target, offset++, 1, 16);
		inAChem = GenUtils.safeGet(target, offset++);
		inBAmount = GenUtils.safeGet(target, offset++, 1, 16);
		inBChem = GenUtils.safeGet(target, offset++);
		outAAmount = GenUtils.safeGet(target, offset++, 1, 16);
		outAChem = GenUtils.safeGet(target, offset++);
		outBAmount = GenUtils.safeGet(target, offset++, 1, 16);
		outBChem = GenUtils.safeGet(target, offset++);
		rate = GenUtils.safeGet(target, offset++);
	}

	@Override
	public int getDataLength() {
		return 9;
	}
}
