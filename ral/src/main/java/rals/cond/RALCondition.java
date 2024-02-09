/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.cctx.*;
import rals.expr.*;
import rals.types.*;

/**
 * Condition.
 * Note that there's no unresolved form, as that's just RALExprUR.
 */
public abstract class RALCondition extends RALExprSlice {
	public final RALType bool;
	public final RALSlot boolRSlot;
	public RALCondition(TypeSystem ts) {
		super(1);
		bool = ts.gBoolean;
		boolRSlot = new RALSlot(bool, RALSlot.Perm.R);
	}

	/**
	 * Coerces the input to a condition by one of two methods:
	 * + Returns it as-is if it's a condition
	 * + Turns it into a != 0 clause otherwise
	 * Also checks the type is sane.
	 */
	public static RALCondition coerceToCondition(RALExprSlice re, TypeSystem ts) {
		if (re instanceof RALCondition)
			return (RALCondition) re;
		if (re instanceof RALConditionCoercable)
			return ((RALConditionCoercable) re).coerceToCondition();
		re.assert1ReadType().assertImpCast(ts.gBoolean);
		return RALCondSimple.Resolved.of(ts, RALCondSimple.Op.NotEqual, re, new RALConstant.Int(ts, 0));
	}

	/**
	 * Coerces a constant.
	 */
	public static boolean constToBool(RALConstant rcR) {
		if (rcR instanceof RALConstant.Flo)
			return ((RALConstant.Flo) rcR).value != 0;
		if (rcR instanceof RALConstant.Int)
			return ((RALConstant.Int) rcR).value != 0;
		throw new RuntimeException("Attempt to coerce constant " + rcR + " to boolean");
	}

	public static RALConstant boolToConst(TypeSystem ts, boolean b) {
		return new RALConstant.Int(ts.gBoolean, b ? 1 : 0);
	}

	/**
	 * Compiles a condition. The CAOS condition code is returned.
	 * writer writes into the prelude.
	 * sharedContext is a context held between the prelude and the use of the condition.
	 */
	public abstract String compileCond(CodeWriter writer, CompileContext sharedContext, boolean invert);

	@Override
	protected final void readCompileInner(RALExprSlice out, CompileContext context) {
		// [CAOS]
		String cc = compileCond(context.writer, context, false);
		context.writer.writeCode("doif " + cc, 1);
		out.writeCompile(0, "1", RALType.Major.Value, context);
		context.writer.writeCode(-1, "else", 1);
		out.writeCompile(0, "0", RALType.Major.Value, context);
		context.writer.writeCode(-1, "endi");
	}

	@Override
	protected final RALSlot slotInner(int index) {
		return boolRSlot;
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
