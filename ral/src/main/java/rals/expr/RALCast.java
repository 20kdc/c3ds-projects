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
				((RALConstant.Single) rc).type.implicitlyCastOrThrow(target);
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
	public RALExpr resolve(ScopeContext context) {
		return Resolved.of(base.resolve(context), target, doImplicitCheck);
	}

	public static class Denull implements RALExprUR {
		public final RALExprUR base;

		public Denull(RALExprUR b) {
			base = b;
		}

		@Override
		public RALExpr resolve(ScopeContext context) {
			RALExpr r = base.resolve(context);
			RALType nn = r.assertOutTypeSingle();
			// System.out.println(nn);
			nn = context.script.typeSystem.byNonNullable(nn);
			// System.out.println(nn);
			return Resolved.of(r, nn, false);
		}
	}

	public static class Resolved implements RALExpr {
		public final RALExpr expr;
		public final RALType target;
		private final boolean doImplicitCheck;
		private Resolved(RALExpr e, RALType t, boolean c) {
			expr = e;
			target = t;
			doImplicitCheck = c;
		}

		/**
		 * Tries to prevent layers from piling up unnecessarily.
		 */
		public static Resolved of(RALExpr e, RALType t, boolean doImplicitCheck) {
			if (e instanceof Resolved)
				e = ((Resolved) e).expr;
			return new Resolved(e, t, doImplicitCheck);
		}

		@Override
		public String toString() {
			return "Cast" + (doImplicitCheck ? "Imp" : "") + "[" + expr + "!" + target + "]";
		}

		@Override
		public RALType[] outTypes() {
			// make sure the length is right
			// also ensure implicit cast is possible if we want that
			if (doImplicitCheck) {
				expr.assertOutTypeSingleImpcast(target);
			} else {
				expr.assertOutTypeSingle();
			}
			return new RALType[] {target};
		}

		@Override
		public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
			// Invert ourselves so we apply to the target.
			// This is important because it ensures we overwrite inputExactType for storage.
			expr.outCompile(writer, new RALExpr[] {new Resolved(out[0], target, doImplicitCheck)}, context);
		}

		@Override
		public RALType inType() {
			// Useful for throwing assertions.
			RALType rt = expr.inType();
			if (doImplicitCheck)
				target.implicitlyCastOrThrow(rt);
			return target;
		}

		@Override
		public void inCompile(CodeWriter writer, String input, RALType inputExactType, CompileContext context) {
			// Overwriting inputExactType here is what turns, i.e. null|integer (major type unknown) into integer (Int).
			// This is important for set instruction selection.
			expr.inCompile(writer, input, doImplicitCheck ? inputExactType : target, context);
		}

		@Override
		public String getInlineCAOS(CompileContext context, boolean write) {
			return expr.getInlineCAOS(context, write);
		}

		@Override
		public SpecialInline getSpecialInline(CompileContext context) {
			return expr.getSpecialInline(context);
		}
	}
}
