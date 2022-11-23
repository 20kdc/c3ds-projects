/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.CompileContextNW;
import rals.types.RALType;

/**
 * For expressions that are deferred until later...
 */
public abstract class RALDeferredExpr extends RALExprSlice {
	public final int base;
	public final RALType[] readTypes, writeTypes;

	public RALDeferredExpr(int b, int l, RALType[] r, RALType[] w) {
		super(l);
		base = b;
		readTypes = r;
		writeTypes = w;
	}

	@Override
	protected RALExprSlice sliceInner(int b, int l) {
		return new Sliced(base + b, l, readTypes, writeTypes);
	}

	@Override
	protected RALType readTypeInner(int index) {
		if (readTypes == null)
			super.readTypeInner(index);
		return readTypes[base + index];
	}

	@Override
	protected RALType writeTypeInner(int index) {
		// Yes, this can be unchecked.
		// This is the reason I'm so paranoid about making sure compile functions throw.
		if (writeTypes == null)
			super.writeTypeInner(index);
		return writeTypes[base + index];
	}

	public abstract RALExprSlice getUnderlying(CompileContextNW cc);

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		getUnderlying(context).readCompile(out, context);
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		getUnderlying(context).writeCompile(index, input, inputExactType, context);
	}

	@Override
	protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
		return getUnderlying(context).getInlineCAOS(index, write, context);
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
		return getUnderlying(context).getSpecialInlineInner(index, context);
	}

	public class Sliced extends RALDeferredExpr {
		public Sliced(int b, int l, RALType[] rt, RALType[] wt) {
			super(b, l, rt, wt);
		}

		@Override
		protected RALExprSlice sliceInner(int b, int l) {
			return RALDeferredExpr.this.sliceInner(base + b, l);
		}

		@Override
		public RALExprSlice getUnderlying(CompileContextNW cc) {
			return RALDeferredExpr.this.getUnderlying(cc).slice(base, length);
		}
	}
}