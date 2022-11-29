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
	public final boolean negate;

	public RALBitInvert(RALExprUR interior, boolean n) {
		expr = interior;
		negate = n;
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant rc = expr.resolveConst(ts);
		if (rc == null)
			return null;
		if (rc instanceof RALConstant.Int)
			return new RALConstant.Int(ts, negate ? -((RALConstant.Int) rc).value : ~((RALConstant.Int) rc).value);
		if (negate)
			if (rc instanceof RALConstant.Flo)
				return new RALConstant.Flo(ts, -((RALConstant.Flo) rc).value);
		return null;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final RALExprSlice exprR = expr.resolve(scope);
		final RALType numT = exprR.assert1ReadType();
		numT.assertImpCast(negate ? scope.world.types.gNumber : scope.world.types.gInteger);
		final RALSlot rs = new RALSlot(numT, RALSlot.Perm.R);
		final String actualCmd = negate ? "negv" : "notv";
		return new RALExprSlice(1) {
			@Override
			public String toString() {
				return actualCmd + "[" + exprR + "]";
			}

			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				if (out.getSpecialInline(0, context) == RALSpecialInline.VA) {
					// fast and good
					exprR.readCompileInner(out, context);
					context.writer.writeCode(actualCmd + " " + out.getInlineCAOS(0, true, context));
				} else {
					// slow and bad
					try (CompileContext c2 = new CompileContext(context)) {
						RALVarVA tmp = c2.allocVA(context.typeSystem.gInteger, "RALBitInvert slowpath tmp");
						String tmpCode = tmp.getCode(c2);
						exprR.readCompileInner(tmp, c2);
						context.writer.writeCode(actualCmd + " " + tmpCode);
						out.writeCompile(0, tmpCode, tmp.type, context);
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
