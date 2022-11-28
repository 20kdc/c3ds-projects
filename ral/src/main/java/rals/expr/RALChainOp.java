/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.LinkedList;

import rals.code.*;
import rals.expr.RALConstant.Int;
import rals.expr.RALConstant.Number;
import rals.types.*;

/**
 * Binary operators!
 * Note that if you were looking for something that works on booleans, you're looking for RALCondLogOp.
 * This isn't that.
 */
public class RALChainOp implements RALExprUR {
	public final RALExprUR[] elements;
	public final Op op;
	public RALChainOp(Op o, RALExprUR[] elm) {
		op = o;
		elements = elm;
	}

	public static RALExprUR of(RALExprUR l, Op thisOp, RALExprUR r) {
		LinkedList<RALExprUR> total = new LinkedList<>();
		boolean handledL = false;
		if (l instanceof RALChainOp) {
			RALChainOp leftCO = (RALChainOp) l;
			if (leftCO.op == thisOp) {
				for (RALExprUR elm : leftCO.elements)
					total.add(elm);
				handledL = true;
			}
		}
		if (!handledL)
			total.add(l);
		total.add(r);
		return new RALChainOp(thisOp, total.toArray(new RALExprUR[0]));
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant res = null;
		for (int i = 0; i < elements.length; i++) {
			RALConstant c = elements[i].resolveConst(ts);
			if (c == null)
				return null;
			if (i == 0) {
				res = c;
			} else {
				res = op.stepConst(ts, res, c);
			}
		}
		return res;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		RALExprSlice[] allArgSlices = new RALExprSlice[elements.length];
		RALType typePipeline = null;
		for (int i = 0; i < elements.length; i++) {
			RALExprSlice slice = elements[i].resolve(scope);
			RALType t = slice.assert1ReadType();
			op.typeCheck(scope.world.types, t);
			if (i == 0) {
				typePipeline = op.startType(scope.world.types, t);
			} else {
				typePipeline = op.stepType(scope.world.types, typePipeline, t);
			}
			allArgSlices[i] = slice;
		}
		final RALType finalType = typePipeline;
		final RALSlot finalSlot = new RALSlot(finalType, RALSlot.Perm.R);
		return new RALExprSlice(1) {
			@Override
			protected RALSlot slotInner(int index) {
				return finalSlot;
			}

			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				// VA fastpath disabled due to bugs, see negation test.
				// In short, the VA fastpath is causing trouble when the input contains the output VA.
				// No good way I can think of to do this.
				// That in mind, the in-place binops might need to be expressed differently.
				// A dedicated superstructure covering ops might be good.
				if (false && (out.getSpecialInline(0, context) == RALSpecialInline.VA)) {
					// VA fastpath.
					// Note we ONLY do this for VAs, because we're not doing an atomic write.
					try (CompileContext cc = new CompileContext(context)) {
						mainCompile(out, out.getInlineCAOS(0, true, cc), cc);
					}
				} else {
					// Temporary value path
					try (CompileContext cc = new CompileContext(context)) {
						RALVarVA fv = cc.allocVA(finalType, "RALChainOp chain tmp");
						mainCompile(fv, fv.getCode(cc), cc);
						fv.readCompile(out, cc);
					}
				}
			}
			private void mainCompile(RALExprSlice out, String outInlineW, CompileContext context) {
				// make sure the initial setup hears whatever it wants to hear
				// because the final type may not be anything like what went in!
				RALType rt = allArgSlices[0].assert1ReadType();
				allArgSlices[0].readCompile(RALCast.Resolved.of(out, rt, false), context);
				// initial type transform
				op.startCodegen(rt, outInlineW, context);
				rt = op.startType(context.typeSystem, rt);
				// alright, ready to go
				RALVarVA tmpVA = null;
				for (int i = 1; i < allArgSlices.length; i++) {
					RALExprSlice other = allArgSlices[i];
					String otherInlineR = other.getInlineCAOS(0, false, context);
					if (otherInlineR == null) {
						if (tmpVA == null)
							tmpVA = context.allocVA(context.typeSystem.gAny, "RALChainOp read tmp");
						// in the interest of helping maintain sanity, let's indent this
						context.writer.indent++;
						other.readCompile(tmpVA, context);
						context.writer.indent--;
						otherInlineR = tmpVA.getCode(context);
					}
					RALType otherType = other.assert1ReadType();
					op.stepCodegen(rt, outInlineW, otherType, otherInlineR, context);
					rt = op.stepType(context.typeSystem, rt, otherType);
				}
			}
		};
	}

	public abstract static class Op {
		// These only matter for ADD_STR
		void startCodegen(RALType rt, String outInlineW, CompileContext context) {
		}
		RALType startType(TypeSystem ts, RALType rt) {
			return rt;
		}
		RALConstant startConst(TypeSystem ts, RALConstant l) {
			return l;
		}
		// Regular
		abstract void stepCodegen(RALType l, String lInline, RALType r, String rInline, CompileContext context);
		abstract RALType stepType(TypeSystem ts, RALType l, RALType r);
		abstract RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r);
		// Typecheck
		abstract void typeCheck(TypeSystem ts, RALType rt);
	}
	private abstract static class BasicOp extends Op {
		final String modifier;
		BasicOp(String m) {
			modifier = m;
		}
		@Override
		void stepCodegen(RALType l, String lInline, RALType r, String rInline, CompileContext context) {
			context.writer.writeCode(modifier + " " + lInline + " " + rInline);
		}
		@Override
		void typeCheck(TypeSystem ts, RALType rt) {
			rt.assertImpCast(ts.gNumber);
		}
	}
	private abstract static class BitNumberOp extends BasicOp {
		BitNumberOp(String m) {
			super(m);
		}
		@Override
		void typeCheck(TypeSystem ts, RALType rt) {
			rt.assertImpCast(ts.gInteger);
		}
		@Override
		RALType stepType(TypeSystem ts, RALType l, RALType r) {
			return ts.gInteger;
		}
		final RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r) {
			if ((l instanceof RALConstant.Int) && (r instanceof RALConstant.Int))
				return new RALConstant.Int(ts, stepConst(((Int) l).value, ((Int) r).value));
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
		RALType stepType(TypeSystem ts, RALType l, RALType r) {
			return workOutNumberType(ts, l, r);
		}
		final RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r) {
			if ((l instanceof Number) && (r instanceof Number)) {
				if ((l instanceof Int) && (r instanceof Int))
					return new Int(ts, stepConstI(((Int) l).value, ((Int) r).value));
				return new RALConstant.Flo(ts, stepConstF(((Number) l).toFloat(), ((Number) r).toFloat()));
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

	public static final Op ADD = new AddOp(false);

	// This is specifically for string embeddings
	public static final Op ADD_STR = new AddOp(true);

	private static class AddOp extends Op {
		public final boolean strOnly;
		AddOp(boolean stringsOnly) {
			strOnly = stringsOnly;
		}
		@Override
		void typeCheck(TypeSystem ts, RALType rt) {
			boolean str = rt.canImplicitlyCast(ts.gString);
			if (!str)
				rt.assertImpCast(ts.gNumber);
		}
		@Override
		void startCodegen(RALType rt, String outInlineW, CompileContext context) {
			if (strOnly)
				if (!rt.canImplicitlyCast(context.typeSystem.gString))
					context.writer.writeCode("sets " + outInlineW + " vtos " + outInlineW);
		}
		@Override
		RALType startType(TypeSystem ts, RALType rt) {
			if (!strOnly)
				return rt;
			return ts.gString; 
		}
		@Override
		RALConstant startConst(TypeSystem ts, RALConstant l) {
			if (strOnly)
				if (l instanceof RALConstant.Number)
					return new RALConstant.Str(ts, l.toString());
			return l;
		}
		@Override
		void stepCodegen(RALType l, String lInline, RALType r, String rInline, CompileContext context) {
			boolean lStr = l.canImplicitlyCast(context.typeSystem.gString);
			boolean rStr = r.canImplicitlyCast(context.typeSystem.gString);
			if ((!lStr) && (!rStr)) {
				// NUM + NUM
				if (strOnly) {
					// This shouldn't actually happen, because the initial value step fixes this.
					// Pretend we didn't notice that.
					context.writer.writeCode("sets " + lInline + " vtos " + lInline);
					context.writer.writeCode("adds " + lInline + " vtos " + rInline);
				} else {
					context.writer.writeCode("addv " + lInline + " " + rInline);
				}
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
		RALConstant stepConst(TypeSystem ts, RALConstant l, RALConstant r) {
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
			if (strOnly)
				return new RALConstant.Str(ts, l.toString() + r.toString());
			if ((l instanceof RALConstant.Int) && (r instanceof RALConstant.Int))
				return new RALConstant.Int(ts, ((RALConstant.Int) l).value + ((RALConstant.Int) r).value);
			return new RALConstant.Flo(ts, l.toFloat() + r.toFloat());
		}
		@Override
		RALType stepType(TypeSystem ts, RALType l, RALType r) {
			boolean lStr = l.canImplicitlyCast(ts.gString);
			boolean rStr = r.canImplicitlyCast(ts.gString);
			if (!lStr)
				l.assertImpCast(ts.gNumber);
			if (!rStr)
				r.assertImpCast(ts.gNumber);
			if (strOnly || lStr || rStr)
				return ts.gString;
			// neither is string, so it's NUM + NUM, so use that logic
			return workOutNumberType(ts, l, r);
		}
	};
}
