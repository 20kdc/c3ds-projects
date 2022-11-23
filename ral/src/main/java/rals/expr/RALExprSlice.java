/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.types.*;

/**
 * Part of a general redesign of RALExpr.
 * 
 * <i>This file essentially defines the core model of how RAL expressions work.
 * Be careful with it!</i>
 * 
 * KNOWN CONCERNS:
 * + If a side-effect-causing expression is sliced away, the side effects are also lost.
 * + Read/write APIs are massive (but seem necessary).
 * + Is there ever a situation where the read type is NOT the write type?
 *   Consider folding this and exposing flags for available operations and side effects.
 */
public abstract class RALExprSlice {
	public static final RALExprSlice EMPTY = new Empty();
	public final int length;

	public RALExprSlice(int l) {
		length = l;
	}

	// -- Public Wrappers --

	/**
	 * Returns the readable type of a given slot in this slice.
	 * Also used to test readability (throws exception if not readable)
	 */
	public final RALType readType(int index) {
		checkSlot(index);
		return readTypeInner(index);
	}

	/**
	 * Compiles a read of this expression slice, which writes into the given output expression slice.
	 * Must throw if not readable.
	 */
	public final void readCompile(RALExprSlice out, CompileContext context) {
		if (out.length != length)
			throw new RuntimeException("Attempted to read " + this + " directly to " + out + " (different lengths!)");
		getUnderlying(context).readCompileInner(out, context);
	}

	/**
	 * Returns the writable type of a given slot in this slice.
	 * Also used to test writability (throws exception if not writable)
	 */
	public final RALType writeType(int index) {
		checkSlot(index);
		return writeTypeInner(index);
	}

	/**
	 * Compiles a write.
	 * WARNING: May alter TARG before input runs. If this matters, make a temporary.
	 * Must throw if not writable, or else expression handles (return-values-as-variables) will break.
	 */
	public final void writeCompile(int index, String input, RALType inputExactType, CompileContext context) {
		checkSlot(index);
		getUnderlying(context).writeCompileInner(index, input, inputExactType, context);
	}

	/**
	 * Gets the inline CAOS for this expression, or null if that's not possible.
	 * NOTE: This mustn't throw because a write was requested. Always return null for impossible operations.
	 * This acts as a "fast-path" to avoid temporary variables.
	 * It's also critical to how inline statements let you modify variables, hence the name.
	 */
	public final String getInlineCAOS(int index, boolean write, CompileContextNW context) {
		checkSlot(index);
		return getUnderlying(context).getInlineCAOSInner(index, write, context);
	}

	/**
	 * Like getInlineCAOS but better.
	 */
	public final RALSpecialInline getSpecialInline(int index, CompileContextNW context) {
		checkSlot(index);
		return getUnderlying(context).getSpecialInlineInner(index, context);
	}

	// -- Public API --

	@Override
	public String toString() {
		return getClass().getName();
	}

	/**
	 * Throws an error if the length isn't 1, then returns readType(0).
	 */
	public final RALType assert1ReadType() {
		if (length != 1)
			throw new RuntimeException("Failed assert1ReadType: " + this);
		return readTypeInner(0);
	}

	/**
	 * Attempts to join together a list of slices.
	 */
	public static RALExprSlice concat(RALExprSlice... slices) {
		RALExprSlice basis = EMPTY;
		for (RALExprSlice res : slices)
			basis = concat(basis, res);
		return basis;
	}

	/**
	 * Joins together a pair of slices.
	 */
	public static RALExprSlice concat(RALExprSlice a, RALExprSlice b) {
		// try the most obvious
		RALExprSlice res = a.tryConcatWithInner(b);
		if (res != null)
			return res;
		// concatenate lists are right-deep to make this easier to handle
		if (b instanceof Concatenate) {
			RALExprSlice aba = a.tryConcatWithInner(((Concatenate) b).a);
			if (aba != null) {
				// Note we recurse in case there's more we can do here.
				return concat(aba, ((Concatenate) b).b);
			}
		}
		// before actually using a full concatenate, handle the blatant cases
		if (a.length == 0)
			return b;
		if (b.length == 0)
			return a;
		return new Concatenate(a, b);
	}

	/**
	 * Attempts to slice this expression slice.
	 */
	public final RALExprSlice slice(int base, int newLen) {
		if (newLen == 0)
			return EMPTY;
		if (newLen < 0)
			throw new IndexOutOfBoundsException("Cannot slice " + this + " with a < 0 length " + length);
		if (base < 0)
			throw new IndexOutOfBoundsException("Attempted to slice at base " + base + " < 0 of " + this);
		if (base + newLen > length)
			throw new IndexOutOfBoundsException("Attempted to slice " + base + "[" + newLen + "]" + " > " + length + " of " + this);
		// then it must cover the whole thing, so...
		if (newLen == length)
			return this;
		return sliceInner(base, newLen);
	}

	// -- Internal API --

	/**
	 * This divorces the type signature of a slice from it's underlying implementation (xCompile/xInline functions).
	 * This is important for expressions where:
	 * 1. The type signature is known
	 * 2. The expression won't exist until later
	 */
	protected RALExprSlice getUnderlying(CompileContextNW context) {
		return this;
	}

	/**
	 * Implementation of slicing this expression slice.
	 */
	protected RALExprSlice sliceInner(int base, int length) {
		return new QuasiSlice(this, base, length);
	}

	/**
	 * Implementation of custom concatenation logic.
	 */
	protected RALExprSlice tryConcatWithInner(RALExprSlice b) {
		return null;
	}

	private final void checkSlot(int index) {
		if (index < 0 || index >= length)
			throw new IndexOutOfBoundsException("Invalid slot " + index + " in " + this);
	}

	/**
	 * Returns the readable type of a given slot in this slice.
	 * Also used to test readability (throws exception if not readable)
	 */
	protected RALType readTypeInner(int index) {
		throw new RuntimeException("Read not supported on " + this);
	}

	/**
	 * Compiles a read of this expression slice, which writes into the given output expression slice.
	 * Must throw if not readable.
	 */
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		throw new RuntimeException("Read not supported on " + this);
	}

	/**
	 * Returns the writable type of a given slot in this slice.
	 * Also used to test writability (throws exception if not writable)
	 */
	protected RALType writeTypeInner(int index) {
		throw new RuntimeException("Write not supported on " + this);
	}

	/**
	 * Compiles a write.
	 * WARNING: May alter TARG before input runs. If this matters, make a temporary.
	 * Must throw if not writable, or else expression handles (return-values-as-variables) will break.
	 */
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		throw new RuntimeException("Write not supported on " + this);
	}

	/**
	 * Gets the inline CAOS for this expression, or null if that's not possible.
	 * NOTE: This mustn't throw because a write was requested. Always return null for impossible operations.
	 * This acts as a "fast-path" to avoid temporary variables.
	 * It's also critical to how inline statements let you modify variables, hence the name.
	 */
	protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
		RALSpecialInline si = getSpecialInline(index, context);
		if (write && !si.inlineWritable)
			return null;
		return si.code;
	}

	/**
	 * Like getInlineCAOS but better.
	 */
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
		return RALSpecialInline.None;
	}

	/**
	 * The empty slice.
	 * Private as you shouldn't instanceof this.
	 */
	private static final class Empty extends RALExprSlice {
		Empty() {
			super(0);
		}
		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			// Reading is supported (but NOP) because someone could pass in an empty slice.
			// Think: () = ()
			// This assignment is perfectly logically reasonable.
		}
	}

	/**
	 * Deferred. The actual resolved instance doesn't exist yet (but the types do).
	 */
	public static abstract class Deferred extends RALExprSlice {
		public final int base;
		public final RALType[] readTypes, writeTypes;

		public Deferred(int b, int l, RALType[] r, RALType[] w) {
			super(l);
			base = b;
			readTypes = r;
			writeTypes = w;
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
	}

	/**
	 * Private as you still shouldn't instanceof this.
	 * Note that these are forced to be right-deep.
	 */
	private static final class Concatenate extends RALExprSlice {
		final RALExprSlice a, b;

		Concatenate(RALExprSlice ra, RALExprSlice rb) {
			super(ra.length + rb.length);
			if (ra instanceof Concatenate) {
				RALExprSlice raA = ((Concatenate) ra).a;
				RALExprSlice raB = ((Concatenate) ra).b;
				a = raA;
				b = new Concatenate(raB, rb);
			} else {
				a = ra;
				b = rb;
			}
		}

		@Override
		public RALExprSlice getUnderlying(CompileContextNW context) {
			RALExprSlice mA = a.getUnderlying(context);
			RALExprSlice mB = b.getUnderlying(context);
			if (mA != a || mB != b)
				return concat(mA, mB);
			return this;
		}

		@Override
		protected RALType readTypeInner(int index) {
			if (index < a.length) {
				return a.readTypeInner(index);
			} else {
				return b.readTypeInner(index - a.length);
			}
		}

		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			RALExprSlice outASlice = out.slice(0, a.length);
			RALExprSlice outBSlice = out.slice(a.length, b.length);
			a.readCompileInner(outASlice, context);
			b.readCompileInner(outBSlice, context);
		}

		@Override
		protected RALType writeTypeInner(int index) {
			if (index < a.length) {
				return a.writeTypeInner(index);
			} else {
				return b.writeTypeInner(index - a.length);
			}
		}

		@Override
		protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
			if (index < a.length) {
				a.writeCompileInner(index, input, inputExactType, context);
			} else {
				b.writeCompileInner(index - a.length, input, inputExactType, context);
			}
		}

		@Override
		protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			if (index < a.length) {
				return a.getInlineCAOSInner(index, write, context);
			} else {
				return b.getInlineCAOSInner(index - a.length, write, context);
			}
		}

		@Override
		protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
			if (index < a.length) {
				return a.getSpecialInlineInner(index, context);
			} else {
				return b.getSpecialInlineInner(index - a.length, context);
			}
		}

		@Override
		protected RALExprSlice sliceInner(int base, int length) {
			// Check for slice being wholly in either base
			// (this catches all the edge cases the below won't handle)
			if (base >= a.length) {
				return b.slice(base - a.length, length);
			} else if (base + length <= a.length) {
				return a.slice(base, length);
			}
			// Slice definitely crosses both.
			// So calculate A's amount by the amount between base and the A/B barrier.
			int aAmount = a.length - base;
			// And B's amount is whatever remains.
			int bAmount = length - aAmount;
			// And concatenate as so.
			return concat(a.slice(base, aAmount), b.slice(0, bAmount));
		}
	}

	/**
	 * Private as you still shouldn't instanceof this.
	 * This is used for (presently) impossible slices.
	 * Note that the assumption is that bigger slices are more possible than smaller slices.
	 * Note also that getUnderlying may return an object on which the slice is possible.
	 */
	private static final class QuasiSlice extends RALExprSlice {
		final RALExprSlice source;
		final int sliceBase;
		public QuasiSlice(RALExprSlice src, int base, int l) {
			super(l);
			source = src;
			sliceBase = base;
		}

		@Override
		public RALExprSlice getUnderlying(CompileContextNW cc) {
			RALExprSlice res = source.getUnderlying(cc);
			if (res == source)
				return this;
			return res.slice(sliceBase, length);
		}

		@Override
		protected RALType readTypeInner(int index) {
			return source.readTypeInner(sliceBase + index);
		}

		@Override
		protected RALType writeTypeInner(int index) {
			return source.writeTypeInner(sliceBase + index);
		}

		@Override
		protected RALExprSlice sliceInner(int b, int l) {
			return source.slice(sliceBase + b, l);
		}

		@Override
		protected RALExprSlice tryConcatWithInner(RALExprSlice b) {
			if (b instanceof QuasiSlice) {
				QuasiSlice qs = (QuasiSlice) b;
				if (qs.source == source) {
					// see if this attaches
					if (qs.sliceBase == sliceBase + length)
						return source.slice(sliceBase, length + qs.length);
				}
			}
			return super.tryConcatWithInner(b);
		}

		@Override
		public String toString() {
			return "unresolved slice [" + sliceBase + " len " + length + "]: " + source;
		}
	}
}
