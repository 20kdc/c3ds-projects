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
 * Bit inversion
 */
public class RALBitInvert implements RALExprUR {
	public final RALExprUR expr;

	public RALBitInvert(RALExprUR interior) {
		expr = interior;
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant rc = expr.resolveConst(ts);
		if (rc == null)
			return null;
		if (rc instanceof RALConstant.Int)
			return new RALConstant.Int(ts, ~((RALConstant.Int) rc).value);
		return null;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final RALExprSlice exprR = expr.resolve(scope);
		exprR.assert1ReadType().assertImpCast(scope.world.types.gInteger);
		final RALSlot rs = new RALSlot(scope.world.types.gInteger, RALSlot.Perm.R);
		return new RALExprSlice(1) {
			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				if (out.getSpecialInline(0, context) == RALSpecialInline.VA) {
					// fast and good
					super.readCompileInner(out, context);
					context.writer.writeCode("notv " + out.getInlineCAOS(0, true, context));
				} else {
					// slow and bad
					try (CompileContext c2 = new CompileContext(context)) {
						RALVarString.Fixed tmp = c2.allocVA(context.typeSystem.gInteger);
						exprR.readCompileInner(tmp, c2);
						context.writer.writeCode("notv " + tmp.code);
						out.writeCompile(0, tmp.code, tmp.type, context);
					}
				}
			}

			@Override
			protected RALSlot slotInner(int index) {
				return rs;
			}
		};
	}

}
