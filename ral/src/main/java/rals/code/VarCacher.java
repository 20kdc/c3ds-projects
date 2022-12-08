/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.LinkedList;

import rals.cctx.*;
import rals.expr.*;

/**
 * This is used by macros and inline statements to do their scary bidding.
 * Simply put: Variables marked as inline are put into the resulting expression.
 * In order to make this work, you NEED to call writeCacheCode!
 */
public class VarCacher {
	/**
	 * The original expression.
	 */
	public final RALExprSlice baseExpr;

	/**
	 * baseExpr slots to if they're inline or not.
	 */
	public final boolean[] inline;

	/**
	 * If not null and the name isn't null, used for debugging comments.
	 */
	public final String[] names;

	/**
	 * All copies.
	 */
	public final Copy[] copies;

	/**
	 * This is the output with all the variables.
	 * There is a guarantee that every element of this is either:
	 * 1. a RALStringVar
	 * 2. marked inline by the caller
	 */
	public final RALExprSlice finishedOutput;

	public VarCacher(RALExprSlice in, boolean[] il, String[] n) {
		if (in.length != il.length)
			throw new RuntimeException("inline length must equal input slice length");
		if (n != null)
			if (n.length != in.length)
				throw new RuntimeException("names length must equal input slice length");
		baseExpr = in;
		inline = il;
		names = n;
		LinkedList<Copy> copiesL = new LinkedList<>();
		RALExprSlice output = RALExprSlice.EMPTY;
		int currentSeriesBase = 0;
		// Be aware of the rather non-traditional <= here.
		// It's so that a break series can occur on that last entry.
		for (int i = 0; i <= il.length; i++) {
			boolean breakSeries = (i == il.length) || il[i];
			if (breakSeries) {
				if (currentSeriesBase != i) {
					Copy c = new Copy(currentSeriesBase, i - currentSeriesBase, in);
					copiesL.add(c);
					for (RALVarVA va : c.variables)
						output = RALExprSlice.concat(output, va);
				}
				currentSeriesBase = i;
			}
			if (i == il.length)
				break;
			if (il[i]) {
				// Inline, so write in as-is.
				output = RALExprSlice.concat(output, in.slice(i, 1));
				// Then set current series base after here.
				currentSeriesBase = i + 1;
			}
			// If not inline, we just let the series base stay where it is.
			// The next breakSeries will handle this.
		}
		copies = copiesL.toArray(new Copy[0]);
		finishedOutput = output;
	}

	public void writeCacheCode(CompileContext context) {
		for (Copy c : copies) {
			for (int i = 0; i < c.variables.length; i++) {
				RALVarVA va = c.variables[i];
				int v = context.allocVA(va.handle);
				if (names != null) {
					int absI = c.sourceBase + i;
					if (names[absI] != null)
						context.writer.writeComment(CompileContext.vaToString(v) + ": " + names[absI]);
				}
			}
			c.srcSlice.readInplaceCompile(c.variables, context);
		}
	}

	public static class Copy {
		public final int sourceBase;
		public final RALVarVA[] variables;
		public final RALExprSlice srcSlice;
		public Copy(int base, int length, RALExprSlice in) {
			sourceBase = base;
			variables = new RALVarVA[length];
			for (int i = 0; i < length; i++) {
				final int absIndex = i + base;
				variables[i] = new RALVarVA(new IVAHandle() {
					@Override
					public String toString() {
						return "vcache copy va " + absIndex + " from " + in;
					}
				}, in.readType(absIndex));
			}
			srcSlice = in.slice(base, length);
		}
	}
}
