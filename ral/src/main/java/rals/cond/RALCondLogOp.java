/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.expr.RALStringVar;
import rals.types.RALType;

/**
 * Binary logical operator.
 * Contains lots of shenanigans to make sure this works despite odd rules on the CAOS side.
 */
public class RALCondLogOp implements RALExprUR {
	public final RALExprUR left, right;
	public final String logOp;

	public RALCondLogOp(RALExprUR l, String op, RALExprUR r) {
		left = l;
		logOp = op;
		right = r;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		final RALExpr lE = left.resolve(scope);
		lE.assertOutTypeSingleImpcast(scope.script.typeSystem.gBoolean);
		final RALExpr rE = right.resolve(scope);
		rE.assertOutTypeSingleImpcast(scope.script.typeSystem.gBoolean);
		final RALCondition lC = RALCondition.of(lE);
		final RALCondition rC = RALCondition.of(rE);

		return new RALCondition(scope.script.typeSystem) {
			@Override
			public String compileCond(StringBuilder writer, CompileContext sharedContext) {
				if (lC instanceof RALCondition.Clause) {
					if (rC instanceof RALCondition.Clause) {
						// if it's two clauses, we can always do this
						String aInline = lC.compileCond(writer, sharedContext);
						String bInline = rC.compileCond(writer, sharedContext);
						return aInline + " " + logOp + " " + bInline;
					} else {
						// left is a clause and right isn't - lucky that ops are symmetric.
						// swap left and right - CAOS will interpret as ((RIGHT) THIS-OP LEFT)
						String aInline = rC.compileCond(writer, sharedContext);
						String bInline = lC.compileCond(writer, sharedContext);
						return aInline + " " + logOp + " " + bInline;
					}
				} else {
					if (rC instanceof RALCondition.Clause) {
						// right is a clause and left isn't, that's fine actually
						// CAOS will interpret as ((LEFT) THIS-OP RIGHT)
						String aInline = lC.compileCond(writer, sharedContext);
						String bInline = rC.compileCond(writer, sharedContext);
						return aInline + " " + logOp + " " + bInline;
					} else {
						// neither left nor right are clauses, we have to var one (arbitrarily)
						// so that we have a clause on the right
						String aInline = lC.compileCond(writer, sharedContext);
						String bInline = wrapVar(writer, sharedContext, rC);
						return aInline + " " + logOp + " " + bInline;
					}
				}
			}
			private String wrapVar(StringBuilder writer, CompileContext sharedContext, RALCondition rc) {
				// complex condition into var
				RALStringVar tmp = sharedContext.allocVA(bool);
				rc.outCompile(writer, new RALExpr[] {tmp}, sharedContext);
				return tmp.code + " <> 0";
			}
		};
	}

}
