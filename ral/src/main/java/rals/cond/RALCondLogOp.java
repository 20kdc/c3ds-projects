/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.cctx.*;
import rals.code.*;
import rals.expr.*;
import rals.types.*;

/**
 * Binary logical operator.
 * Contains lots of shenanigans to make sure this works despite odd rules on the CAOS side.
 */
public class RALCondLogOp implements RALExprUR {
	public final RALExprUR left, right;
	public final Op logOp;

	public RALCondLogOp(RALExprUR l, Op op, RALExprUR r) {
		left = l;
		logOp = op;
		right = r;
	}

	@Override
	public String toString() {
		return "(" + left + ") " + logOp.code + " (" + right + ")";
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant rcL = left.resolveConst(ts);
		if (rcL == null)
			return null;
		RALConstant rcR = right.resolveConst(ts);
		if (rcR == null)
			return null;
		boolean lB = RALCondition.constToBool(rcL);
		boolean rB = RALCondition.constToBool(rcR);
		switch (logOp) {
		case And:
			return RALCondition.boolToConst(ts, lB && rB);
		case Or:
			return RALCondition.boolToConst(ts, lB || rB);
		default:
			throw new RuntimeException("can't invert this logop " + logOp + ", what'd you do?");
		}
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final RALExprSlice lE = left.resolve(scope);
		final RALExprSlice rE = right.resolve(scope);
		final RALCondition lC = RALCondition.coerceToCondition(lE, scope.world.types);
		final RALCondition rC = RALCondition.coerceToCondition(rE, scope.world.types);

		return new RALCondition(scope.world.types) {
			@Override
			public String compileCond(CodeWriter writer, CompileContext sharedContext, boolean invert) {

				// NOTE:
				// See codeInv's details, but in short, yes, inverting the innards is INTENTIONAL!!!
				String loc = invert ? logOp.codeInv : logOp.code;

				if (lC instanceof RALCondition.Clause) {
					if (rC instanceof RALCondition.Clause) {
						// if it's two clauses, we can always do this
						String aInline = lC.compileCond(writer, sharedContext, invert);
						String bInline = rC.compileCond(writer, sharedContext, invert);
						return aInline + " " + loc + " " + bInline;
					} else {
						// left is a clause and right isn't - lucky that ops are symmetric.
						// swap left and right - CAOS will interpret as ((RIGHT) THIS-OP LEFT)
						String aInline = rC.compileCond(writer, sharedContext, invert);
						String bInline = lC.compileCond(writer, sharedContext, invert);
						return aInline + " " + loc + " " + bInline;
					}
				} else {
					if (rC instanceof RALCondition.Clause) {
						// right is a clause and left isn't, that's fine actually
						// CAOS will interpret as ((LEFT) THIS-OP RIGHT)
						String aInline = lC.compileCond(writer, sharedContext, invert);
						String bInline = rC.compileCond(writer, sharedContext, invert);
						return aInline + " " + loc + " " + bInline;
					} else {
						// neither left nor right are clauses, we have to var one (arbitrarily)
						// so that we have a clause on the right
						String aInline = lC.compileCond(writer, sharedContext, invert);
						String bInline = wrapVar(writer, sharedContext, rC, invert);
						return aInline + " " + loc + " " + bInline;
					}
				}
			}
			private String wrapVar(CodeWriter writer, CompileContext sharedContext, RALCondition rc, boolean invert) {
				// complex condition into var
				RALVarVA tmp = sharedContext.allocVA(bool, "RALCondLogOp tmp");
				rc.readCompile(tmp, sharedContext);
				return tmp.getCode(sharedContext) + (invert ? " eq 0" : " ne 0");
			}

			@Override
			public String toString() {
				return "(" + lC.toString() + ") " + logOp.code + " (" + rC.toString() + ")";
			}
		};
	}

	public static enum Op {
		And("and", "or"),
		Or("or", "and");
		// codeInv is the op such that (L code R) = !(!L codeInv !R)
		// REMEMBER:
		// AND invert:
		//  (A and B) == !(!L or !R)
		// OR invert:
		//  (A or B) == !(!A and !B)
		public final String code, codeInv;
		Op(String c, String ci) {
			code = c;
			codeInv = ci;
		}
	}
}
