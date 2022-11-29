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
 * Typecast.
 */
public final class RALCast implements RALExprUR {
	public final RALExprUR base;
	public final RALType target;
	public final boolean doImplicitCheck;

	private RALCast(RALExprUR b, RALType t) {
		base = b;
		target = t;
		doImplicitCheck = false;
	}

	private RALCast(RALExprUR b, RALType t, boolean imp) {
		base = b;
		target = t;
		doImplicitCheck = imp;
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant rc = base.resolveConst(ts);
		if (rc instanceof RALConstant.Single) {
			if (doImplicitCheck)
				((RALConstant.Single) rc).type.assertImpCast(target);
			return ((RALConstant.Single) rc).cast(target);
		}
		return null;
	}

	/**
	 * Tries to prevent layers from piling up unnecessarily.
	 */
	public static RALCast of(RALExprUR bx, RALType t) {
		return of(bx, t, false);
	}

	/**
	 * Tries to prevent layers from piling up unnecessarily.
	 */
	public static RALCast of(RALExprUR bx, RALType t, boolean checked) {
		// Skip layers of unchecked casts.
		// Checked ones we keep.
		if (bx instanceof RALCast)
			if (!((RALCast) bx).doImplicitCheck)
				bx = ((RALCast) bx).base;
		return new RALCast(bx, t, checked);
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		return Resolved.of(base.resolve(context), target, doImplicitCheck);
	}

	public static class Denull implements RALExprUR {
		public final RALExprUR base;

		public Denull(RALExprUR b) {
			base = b;
		}

		@Override
		public RALExprSlice resolveInner(ScopeContext context) {
			RALExprSlice r = base.resolve(context);
			RALType nn = r.assert1ReadType();
			// System.out.println(nn);
			nn = context.world.types.byNonNullable(nn);
			// System.out.println(nn);
			return Resolved.of(r, nn, false);
		}
	}

	public static class Resolved extends RALExprSlice.ThickProxy {
		public final RALExprSlice expr;
		public final RALSlot targetSlot;
		private final boolean doImplicitCheck;
		/**
		 * ONLY call site of this should be Resolved.of
		 * This constructor DOESN'T do the typechecks!!!
		 */
		private Resolved(RALExprSlice e, RALSlot tsl, boolean c) {
			super(1);
			expr = e;
			targetSlot = tsl;
			doImplicitCheck = c;
		}

		/**
		 * Tries to prevent layers from piling up unnecessarily.
		 */
		public static Resolved of(RALExprSlice originalExpr, RALType targetType, boolean doImplicitCheck) {
			// Start off with the early sanity checks
			final RALSlot sourceSlot = originalExpr.slot(0);
			if (originalExpr.length != 1)
				throw new RuntimeException("Cannot cast " + originalExpr + " which has length of " + originalExpr.length + ".");
			RALType sourceType = sourceSlot.type;
			// If an implicit cast, translate casting to permissions
			RALSlot.Perm adjustedPerm = sourceSlot.perms;
			if (doImplicitCheck) {
				if (!targetType.canImplicitlyCast(sourceType))
					adjustedPerm = adjustedPerm.denyWrite();
				if (!sourceType.canImplicitlyCast(targetType))
					adjustedPerm = adjustedPerm.denyRead();
			}
			// This is the true slice, (to prevent layering casts over each other)
			// Note this is saved until the end, as permission/type calculations need to happen with the original input
			final RALExprSlice trueSlice = (originalExpr instanceof Resolved) ? ((Resolved) originalExpr).expr : originalExpr;
			return new Resolved(trueSlice, new RALSlot(targetType, adjustedPerm), doImplicitCheck);
		}

		@Override
		public String toString() {
			return "Cast" + (doImplicitCheck ? "Imp" : "") + "[" + expr + "!" + targetSlot + "]";
		}

		@Override
		protected RALSlot slotInner(int index) {
			return targetSlot;
		}

		@Override
		public void readCompileInner(RALExprSlice out, CompileContext context) {
			// just to run the checks, because we have to run them late
			readType(0);
			// Invert ourselves so we apply to the target.
			// This is important because it ensures we overwrite inputExactType for storage.
			expr.readCompile(of(out, targetSlot.type, doImplicitCheck), context);
		}

		@Override
		protected void readInplaceCompileInner(RALVarVA[] out, CompileContext context) {
			// trigger checks
			// in-place compile won't let us use anything other than RALVarVA, so we have to do the cast *immediately*
			// luckily, that's fine, we just do it
			// that in mind, confirm the type casts properly
			RALType rt = readType(0);
			if (doImplicitCheck)
				rt.assertImpCast(out[0].type);
			expr.readInplaceCompile(new RALVarVA[] {new RALVarVA(out[0].handle, rt)}, context);
		}

		@Override
		public void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
			// trigger checks
			writeType(0);
			// Overwriting inputExactType here is what turns, i.e. null|integer (major type unknown) into integer (Int).
			// This is important for set instruction selection.
			expr.writeCompile(0, input, doImplicitCheck ? inputExactType : targetSlot.type, context);
		}

		@Override
		public String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			return expr.getInlineCAOS(index, write, context);
		}

		@Override
		public RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
			return expr.getSpecialInline(index, context);
		}
	}
}
