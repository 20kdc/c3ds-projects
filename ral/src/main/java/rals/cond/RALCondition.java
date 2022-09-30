/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.code.CompileContext;
import rals.expr.RALExpr;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * Condition.
 * Note that there's no unresolved form, as that's just RALExprUR.
 */
public abstract class RALCondition implements RALExpr {
	public final RALType bool;
	public RALCondition(TypeSystem ts) {
		bool = ts.gBoolean;
	}

	public static RALCondition of(RALExpr re) {
		if (re instanceof RALCondition)
			return (RALCondition) re;
		throw new RuntimeException("coercion not implemented");
	}

	/**
	 * Compiles a condition. The CAOS condition code is returned.
	 * writer writes into the prelude.
	 * sharedContext is a context held between the prelude and the use of the condition.
	 */
	public abstract String compileCond(StringBuilder writer, CompileContext sharedContext);

	@Override
	public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
		throw new RuntimeException("Can't write into condition");
	}

	@Override
	public RALType inType() {
		throw new RuntimeException("Can't write into condition");
	}

	@Override
	public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
		String cc = compileCond(writer, context);
		writer.append("doif ");
		writer.append(cc);
		writer.append("\n");
		out[0].inCompile(writer, "1", bool, context);
		writer.append("else\n");
		out[0].inCompile(writer, "0", bool, context);
		writer.append("endi\n");
	}

	@Override
	public RALType[] outTypes() {
		return new RALType[] {bool};
	}

	/**
	 * Implies this fits snugly into a branch of a condition. 
	 */
	public static abstract class Clause extends RALCondition {
		public Clause(TypeSystem ts) {
			super(ts);
		}
	}
}
