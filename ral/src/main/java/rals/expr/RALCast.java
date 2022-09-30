/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.io.StringWriter;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * Typecast.
 */
public final class RALCast implements RALExprUR {
	public final RALExprUR base;
	public final RALType target;

	private RALCast(RALExprUR b, RALType t) {
		base = b;
		target = t;
	}

	/**
	 * Tries to prevent layers from piling up unnecessarily.
	 */
	public static RALCast of(RALExprUR bx, RALType t) {
		if (bx instanceof RALCast)
			bx = ((RALCast) bx).base;
		return new RALCast(bx, t);
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		return Resolved.of(base.resolve(context), target);
	}

	public static class Denull implements RALExprUR {
		public final RALExprUR base;

		public Denull(RALExprUR b) {
			base = b;
		}

		@Override
		public RALExpr resolve(ScopeContext context) {
			RALExpr r = base.resolve(context);
			RALType[] rt = r.outTypes();
			if (rt.length != 1)
				throw new RuntimeException("Can't denull >1 value");
			RALType nn = rt[0];
			// System.out.println(nn);
			nn = context.script.typeSystem.byNonNullable(nn);
			// System.out.println(nn);
			return Resolved.of(r, nn);
		}
	}

	public static class Resolved implements RALExpr {
		public final RALExpr expr;
		public final RALType target;
		private Resolved(RALExpr e, RALType t) {
			expr = e;
			target = t;
		}

		/**
		 * Tries to prevent layers from piling up unnecessarily.
		 */
		public static Resolved of(RALExpr e, RALType t) {
			if (e instanceof Resolved)
				e = ((Resolved) e).expr;
			return new Resolved(e, t);
		}

		@Override
		public String toString() {
			return "Cast[" + expr + "!" + target + "]";
		}

		@Override
		public RALType[] outTypes() {
			RALType[] t = expr.outTypes();
			if (t.length != 1)
				throw new RuntimeException("Can't cast this, not a single type!");
			return new RALType[] {target};
		}

		@Override
		public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
			// Invert ourselves so we apply to the target.
			// This is important because it ensures we overwrite inputExactType for storage.
			expr.outCompile(writer, new RALExpr[] {new Resolved(out[0], target)}, context);
		}

		@Override
		public RALType inType() {
			// Useful for throwing assertions.
			expr.inType();
			return target;
		}

		@Override
		public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
			// Overwriting inputExactType here is what turns, i.e. null|integer (major type unknown) into integer (Int).
			// This is important for set instruction selection.
			expr.inCompile(writer, input, target, context);
		}

		@Override
		public String getInlineCAOS(CompileContext context) {
			return expr.getInlineCAOS(context);
		}
	}
}
