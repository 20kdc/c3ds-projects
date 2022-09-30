/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * Typecast.
 */
public class RALCast implements RALExprUR {
	public final RALExprUR base;
	public final RALType target;

	public RALCast(RALExprUR b, RALType t) {
		base = b;
		target = t;
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		return new Resolved(base.resolve(context), target);
	}

	public static class Denull implements RALExprUR {
		public final RALExprUR base;

		public Denull(RALExprUR b) {
			base = b;
		}

		@Override
		public RALExpr resolve(ScopeContext context) {
			RALExpr r = base.resolve(context);
			RALType[] rt = r.outTypes(context.script);
			if (rt.length != 1)
				throw new RuntimeException("Can't denull >1 value");
			RALType nn = rt[0];
			// System.out.println(nn);
			nn = context.script.typeSystem.byNonNullable(nn);
			// System.out.println(nn);
			return new Resolved(r, nn);
		}
	}

	public static class Resolved implements RALExpr {
		public final RALExpr expr;
		public final RALType target;
		public Resolved(RALExpr e, RALType t) {
			expr = e;
			target = t;
		}

		@Override
		public String toString() {
			return "Cast[" + expr + "!" + target + "]";
		}

		@Override
		public RALType[] outTypes(ScriptContext context) {
			RALType[] t = expr.outTypes(context);
			if (t.length != 1)
				throw new RuntimeException("Can't cast this, not a single type!");
			return new RALType[] {target};
		}

		@Override
		public void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context) {
			// Invert ourselves so we apply to the target.
			// This is important because it ensures we overwrite inputExactType for storage.
			expr.outCompile(writer, new RALExpr[] {new Resolved(out[0], target)}, context);
		}

		@Override
		public RALType inType(ScriptContext context) {
			// Useful for throwing assertions.
			expr.inType(context);
			return target;
		}

		@Override
		public void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context) {
			// Overwriting inputExactType here is what turns, i.e. null|integer (major type unknown) into integer (Int).
			// This is important for set instruction selection.
			expr.inCompile(writer, input, target, context);
		}

		@Override
		public String getInlineCAOS() {
			return expr.getInlineCAOS();
		}
	}
}
