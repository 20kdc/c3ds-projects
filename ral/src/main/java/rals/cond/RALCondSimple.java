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
import rals.types.RALType.Major;
import rals.types.TypeSystem;

/**
 * Simple CAOS condition clause.
 */
public class RALCondSimple implements RALExprUR {
	public final String centre;
	public final RALExprUR left, right;

	public RALCondSimple(RALExprUR l, String c, RALExprUR r) {
		centre = c;
		left = l;
		right = r;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		final RALExpr lR = left.resolve(scope);
		final RALExpr rR = right.resolve(scope);
		return Resolved.of(scope.script.typeSystem, centre, lR, rR);
	}
	
	public static final class Resolved extends RALCondition.Clause {
		private final String centre;
		private final RALExpr rR;
		private final RALExpr lR;
		private final RALType rT;
		private final RALType lT;

		public Resolved(TypeSystem ts, String c, RALExpr rR, RALExpr lR, RALType rT, RALType lT) {
			super(ts);
			centre = c;
			this.rR = rR;
			this.lR = lR;
			this.rT = rT;
			this.lT = lT;
		}

		public static Resolved of(TypeSystem ts, String centre, RALExpr lR, RALExpr rR) {
			RALType lT = lR.assertOutTypeSingle();
			RALType rT = rR.assertOutTypeSingle();
			if (lT.majorType != rT.majorType)
				if ((lT.majorType != Major.Unknown) && (rT.majorType != Major.Unknown))
					throw new RuntimeException("major type mismatch in condition (" + lT.majorType + " vs " + rT.majorType + "), VM doesn't like this");
			return new Resolved(ts, centre, rR, lR, rT, lT);
		}

		@Override
		public String compileCond(StringBuilder writer, CompileContext sharedContext) {
			String lInline = lR.getInlineCAOS(sharedContext);
			String rInline = rR.getInlineCAOS(sharedContext);
			if (lInline == null) {
				RALStringVar lV = sharedContext.allocVA(lT);
				lR.outCompile(writer, new RALExpr[] {lV}, sharedContext);
				lInline = lV.code;
			}
			if (rInline == null) {
				RALStringVar rV = sharedContext.allocVA(rT);
				rR.outCompile(writer, new RALExpr[] {rV}, sharedContext);
				rInline = rV.code;
			}
			return lInline + " " + centre + " " + rInline;
		}
	}
}
