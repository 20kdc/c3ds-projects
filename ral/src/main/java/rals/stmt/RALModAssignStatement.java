/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.diag.SrcRange;
import rals.expr.RALConstant;
import rals.expr.RALExprSlice;
import rals.expr.RALExprUR;
import rals.expr.RALSlot;
import rals.expr.RALVarVA;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * A modifying assignment statement.
 * This was separated out of RALChainOp to try and mitigate the performance loss due to the fixes to the big scary bug.
 * The idea for the code structure change is to try and make RALChainOp use RALModAssignStatement.
 */
public class RALModAssignStatement extends RALStatementUR {
	public final RALExprUR target;
	public final RALExprUR source;
	public final Op op;

	public RALModAssignStatement(SrcRange sr, RALExprUR t, RALExprUR s, Op o) {
		super(sr);
		target = t;
		source = s;
		op = o;
	}

	@Override
	protected RALStatement resolveInner(ScopeContext scope) {
		RALExprSlice resL = target.resolve(scope);
		RALExprSlice resR = source.resolve(scope);
		return new Resolved(extent, resL, resR, op, scope.world.types);
	}

	/**
	 * Beware that this does a bit of implicit casting on what it can get away with writing.
	 */
	public static class Resolved extends RALStatement {
		public final RALExprSlice target, source;
		public final RALType targetType, sourceType, targetNewType;
		public final Op op;

		public Resolved(SrcRange extent, RALExprSlice l, RALExprSlice r, Op o, TypeSystem ts) {
			super(extent);
			target = l;
			source = r;
			op = o;
			// types
			RALType opInput = o.inputType(ts);
			targetType = l.assert1With(RALSlot.Perm.RW);
			sourceType = r.assert1ReadType();
			// check types
			sourceType.assertImpCast(opInput);
			targetType.assertImpCast(opInput);
			targetNewType = o.stepType(ts, targetType, sourceType);
			targetNewType.assertImpCast(targetType);
		}

		@Override
		public String toString() {
			return target + " " + op + " " + source;
		}

		@Override
		protected void compileInner(CodeWriter writer, CompileContext context) {
			try (CompileContext c2 = new CompileContext(context)) {
				RALVarVA lVA = null;
				String lInline = target.getInlineCAOS(0, true, c2);
				String rInline = source.getInlineCAOS(0, false, c2);
				if (lInline == null) {
					lVA = c2.allocVA(sourceType, "modAssignL");
					target.readCompile(lVA, c2);
					lInline = lVA.getCode(c2);
				}
				if (rInline == null) {
					RALVarVA va = c2.allocVA(sourceType, "modAssignR");
					source.readCompile(va, c2);
					rInline = va.getCode(c2);
				}
				op.stepCodegen(targetType, lInline, sourceType, rInline, c2);
				if (lVA != null)
					target.writeCompile(0, lVA.getCode(c2), targetNewType, c2);
			}
		}
	}

	public abstract static class Op {
		// Regular
		public abstract void stepCodegen(RALType l, String lInline, RALType r, String rInline, CompileContext context);
		public abstract RALType stepType(TypeSystem ts, RALType l, RALType r);
		public abstract RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r);
		// Typecheck
		public abstract RALType inputType(TypeSystem ts);
	}
	private abstract static class BasicOp extends Op {
		final String modifier;
		BasicOp(String m) {
			modifier = m;
		}
		@Override
		public String toString() {
			return modifier;
		}
		@Override
		public void stepCodegen(RALType l, String lInline, RALType r, String rInline, CompileContext context) {
			context.writer.writeCode(modifier + " " + lInline + " " + rInline);
		}
		@Override
		public RALType inputType(TypeSystem ts) {
			return ts.gNumber;
		}
	}
	private abstract static class BitNumberOp extends BasicOp {
		BitNumberOp(String m) {
			super(m);
		}
		@Override
		public RALType inputType(TypeSystem ts) {
			return ts.gInteger;
		}
		@Override
		public RALType stepType(TypeSystem ts, RALType l, RALType r) {
			return ts.gInteger;
		}
		public final RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r) {
			if ((l instanceof RALConstant.Int) && (r instanceof RALConstant.Int)) {
				RALConstant.Int li = (RALConstant.Int) l;
				RALConstant.Int ri = (RALConstant.Int) r;
				return new RALConstant.Int(ts, stepConst(li.value, ri.value));
			}
			return null;
		}
		abstract int stepConst(int a, int b);
	}
	/*
	 * So the logic here is, Number isn't implicitly castable to Float because of integer division.
	 * So we can't just say everything's a float. And that's how Number became a compiler builtin.
	 */
	private static RALType workOutNumberType(TypeSystem ts, RALType l, RALType r) {
		boolean lInt = l.canImplicitlyCast(ts.gInteger);
		boolean lFloat = l.canImplicitlyCast(ts.gFloat);
		boolean rInt = r.canImplicitlyCast(ts.gInteger);
		boolean rFloat = r.canImplicitlyCast(ts.gFloat);
		if (lInt && rInt)
			return ts.gInteger;
		if (lFloat || rFloat)
			return ts.gFloat;
		return ts.gNumber;
	}
	private abstract static class CastNumberOp extends BasicOp {
		CastNumberOp(String m) {
			super(m);
		}
		@Override
		public RALType stepType(TypeSystem ts, RALType l, RALType r) {
			return workOutNumberType(ts, l, r);
		}
		public final RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r) {
			if ((l instanceof RALConstant.Number) && (r instanceof RALConstant.Number)) {
				RALConstant.Number ln = (RALConstant.Number) l;
				RALConstant.Number rn = (RALConstant.Number) r;
				if ((l instanceof RALConstant.Int) && (r instanceof RALConstant.Int)) {
					RALConstant.Int li = (RALConstant.Int) l;
					RALConstant.Int ri = (RALConstant.Int) r;
					return new RALConstant.Int(ts, stepConstI(li.value, ri.value));
				}
				return new RALConstant.Flo(ts, stepConstF(ln.toFloat(), rn.toFloat()));
			}
			return null;
		}
		abstract float stepConstF(float a, float b);
		abstract int stepConstI(int a, int b);
	}

	public static final Op OR = new BitNumberOp("orrv") {
		@Override
		int stepConst(int a, int b) {
			return a | b;
		}
	};

	public static final Op AND = new BitNumberOp("andv") {
		@Override
		int stepConst(int a, int b) {
			return a & b;
		}
	};

	public static final Op SUB = new CastNumberOp("subv") {
		@Override
		float stepConstF(float a, float b) {
			return a - b;
		}
		@Override
		int stepConstI(int a, int b) {
			return a - b;
		}
	};

	public static final Op DIV = new CastNumberOp("divv") {
		@Override
		float stepConstF(float a, float b) {
			return a / b;
		}
		@Override
		int stepConstI(int a, int b) {
			return a / b;
		}
	};

	public static final Op MUL = new CastNumberOp("mulv") {
		@Override
		float stepConstF(float a, float b) {
			return a * b;
		}
		@Override
		int stepConstI(int a, int b) {
			return a * b;
		}
	};

	/**
	 * Note that this operator does double-duty for both string and numeric additions 
	 */
	public static final Op ADD = new AddOp();

	private static class AddOp extends Op {
		AddOp() {
		}
		@Override
		public String toString() {
			return "+";
		}
		@Override
		public RALType inputType(TypeSystem ts) {
			return ts.gStringOrNumber;
		}
		@Override
		public void stepCodegen(RALType l, String lInline, RALType r, String rInline, CompileContext context) {
			boolean lStr = l.canImplicitlyCast(context.typeSystem.gString);
			boolean rStr = r.canImplicitlyCast(context.typeSystem.gString);
			if ((!lStr) && (!rStr)) {
				// NUM + NUM
				context.writer.writeCode("addv " + lInline + " " + rInline);
			} else if ((!lStr) && (rStr)) {
				// NUM + STR
				context.writer.writeCode("sets " + lInline + " vtos " + lInline);
				context.writer.writeCode("adds " + lInline + " " + rInline);
			} else if ((lStr) && (!rStr)) {
				// STR + NUM
				context.writer.writeCode("adds " + lInline + " vtos " + rInline);
			} else if ((lStr) && (rStr)) {
				// STR + STR
				context.writer.writeCode("adds " + lInline + " " + rInline);
			} else {
				throw new RuntimeException("How'd you manage that? " + lStr + " " + rStr);
			}
		}
		@Override
		public RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r) {
			if (l instanceof RALConstant.Number) {
				if (r instanceof RALConstant.Number) {
					// NUM + NUM
					return stepConstAddNumbers(ts, (RALConstant.Number) l, (RALConstant.Number) r);
				} else if (r instanceof RALConstant.Str) {
					// NUM + STR
					return new RALConstant.Str(ts, l.toString() + ((RALConstant.Str) r).value);
				}
			} else if (l instanceof RALConstant.Str) {
				if (r instanceof RALConstant.Number) {
					// STR + NUM
					return new RALConstant.Str(ts, ((RALConstant.Str) l).value + r.toString());
				} else if (r instanceof RALConstant.Str) {
					// STR + STR
					return new RALConstant.Str(ts, ((RALConstant.Str) l).value + ((RALConstant.Str) r).value);
				}
			}
			return null;
		}
		private RALConstant stepConstAddNumbers(TypeSystem ts, RALConstant.Number l, RALConstant.Number r) {
			if ((l instanceof RALConstant.Int) && (r instanceof RALConstant.Int))
				return new RALConstant.Int(ts, ((RALConstant.Int) l).value + ((RALConstant.Int) r).value);
			return new RALConstant.Flo(ts, l.toFloat() + r.toFloat());
		}
		@Override
		public RALType stepType(TypeSystem ts, RALType l, RALType r) {
			boolean lStr = l.canImplicitlyCast(ts.gString);
			boolean rStr = r.canImplicitlyCast(ts.gString);
			if (!lStr)
				l.assertImpCast(ts.gNumber);
			if (!rStr)
				r.assertImpCast(ts.gNumber);
			if (lStr || rStr)
				return ts.gString;
			// neither is string, so it's NUM + NUM, so use that logic
			return workOutNumberType(ts, l, r);
		}
	};
}
