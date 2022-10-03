/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.code.*;
import rals.expr.*;
import rals.types.*;
import rals.types.RALType.Major;

/**
 * Simple CAOS condition clause.
 */
public class RALCondSimple implements RALExprUR {
	public final Op centre;
	public final RALExprUR left, right;

	public RALCondSimple(RALExprUR l, Op c, RALExprUR r) {
		centre = c;
		left = l;
		right = r;
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant cL = left.resolveConst(ts);
		if (cL == null)
			return null;
		RALConstant cR = right.resolveConst(ts);
		if (cR == null)
			return null;
		if (centre == Op.Equal) {
			return RALCondition.boolToConst(ts, cL.equals(cR));
		} else if (centre == Op.NotEqual) {
			return RALCondition.boolToConst(ts, !cL.equals(cR));
		}
		return null;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final RALExprSlice lR = left.resolve(scope);
		final RALExprSlice rR = right.resolve(scope);
		return Resolved.of(scope.script.typeSystem, centre, lR, rR);
	}
	
	public static final class Resolved extends RALCondition.Clause {
		private final Op centre;
		private final RALExprSlice rR;
		private final RALExprSlice lR;
		private final RALType rT;
		private final RALType lT;

		private Resolved(TypeSystem ts, Op c, RALExprSlice rR, RALExprSlice lR, RALType rT, RALType lT) {
			super(ts);
			centre = c;
			this.rR = rR;
			this.lR = lR;
			this.rT = rT;
			this.lT = lT;
		}

		public static Resolved of(TypeSystem ts, Op centre, RALExprSlice lR, RALExprSlice rR) {
			RALType lT = lR.assert1ReadType();
			RALType rT = rR.assert1ReadType();
			if (lT.majorType != rT.majorType)
				if ((lT.majorType != Major.Unknown) && (rT.majorType != Major.Unknown))
					throw new RuntimeException("major type mismatch in condition (" + lT.majorType + " vs " + rT.majorType + "), VM doesn't like this");
			return new Resolved(ts, centre, rR, lR, rT, lT);
		}

		@Override
		public String compileCond(CodeWriter writer, CompileContext sharedContext, boolean invert) {
			String lInline = lR.getInlineCAOS(0, false, sharedContext);
			String rInline = rR.getInlineCAOS(0, false, sharedContext);
			if (lInline == null) {
				RALVarString.Fixed lV = sharedContext.allocVA(lT);
				lR.readCompile(lV, sharedContext);
				lInline = lV.code;
			}
			if (rInline == null) {
				RALVarString.Fixed rV = sharedContext.allocVA(rT);
				rR.readCompile(rV, sharedContext);
				rInline = rV.code;
			}
			return lInline + " " + (invert ? centre.codeInv : centre.code) + " " + rInline;
		}

		@Override
		public String toString() {
			return "(" + lR + ") " + centre.code + " (" + rR + ")";
		}
	}
	public static enum Op {
		LessThan("lt", "ge"),
		GreaterThan("gt", "le"),
		LessEqual("le", "gt"),
		GreaterEqual("ge", "lt"),
		Equal("eq", "ne"),
		NotEqual("ne", "eq");
		// codeInv is the inverse.
		public final String code, codeInv;
		Op(String c, String ci) {
			code = c;
			codeInv = ci;
		}
	}
}
