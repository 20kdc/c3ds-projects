/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.diag.SrcRange;
import rals.expr.*;
import rals.lex.DefInfo;
import rals.types.*;

/**
 * Let statement.
 * Introduces new locals or shadows existing locals.
 * Note that null can be given in the types array, which means "auto".
 */
public class RALLetStatement extends RALStatementUR {
	public final String[] names;
	public final int[] fixedAlloc;
	public final RALType[] types;
	public final RALExprUR init;

	public RALLetStatement(DefInfo di, String[] n, int[] fa, RALType[] t, RALExprUR i) {
		super(di);
		names = n;
		fixedAlloc = fa;
		types = t;
		init = i;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		RALExprSlice initRes = null;
		RALType[] finalTypes = new RALType[types.length];
		System.arraycopy(types, 0, finalTypes, 0, types.length);
		if (init != null) {
			initRes = init.resolve(scope);
			// Type-check
			if (initRes.length != names.length)
				throw new RuntimeException("Expression return values don't match amount of defined variables");
			for (int i = 0; i < names.length; i++) {
				RALType rt = initRes.readType(i);
				if (finalTypes[i] != null) {
					rt.assertImpCast(finalTypes[i]);
				} else {
					finalTypes[i] = rt;
				}
			}
		} else {
			for (int i = 0; i < names.length; i++)
				if (finalTypes[i] == null)
					throw new RuntimeException("Variable " + names[i] + " auto but no assignment statement");
		}
		RALVarVA[] vars = new RALVarVA[names.length];
		for (int i = 0; i < names.length; i++) {
			RALVarVA rvv = scope.newLocal(names[i], defInfo, finalTypes[i]);
			vars[i] = rvv;
		}
		return new Resolved(extent, names, fixedAlloc, vars, initRes);
	}

	public static class Resolved extends RALStatement {
		public final String[] names;
		public final int[] fixedAlloc;
		public final RALVarVA[] vars;
		public final RALExprSlice init;
	
		public Resolved(SrcRange sp, String[] n, int[] fa, RALVarVA[] v, RALExprSlice i) {
			super(sp);
			names = n;
			fixedAlloc = fa;
			vars = v;
			init = i;
		}
	
		@Override
		protected void compileInner(CodeWriter writer, CompileContext scope) {
			// Ok, so we want to define this local in the outer environment, but carefully.
			// In particular we want to be able to use local definitions as a cast.
			for (int i = 0; i < vars.length; i++) {
				if (fixedAlloc[i] == -1) {
					scope.allocVA(vars[i].handle);
				} else {
					scope.allocVA(vars[i].handle, fixedAlloc[i]);
				}
				writer.writeComment(vars[i].getInlineCAOS(0, false, scope) + ": " + vars[i].type + " " + names[i]);
			}

			if (init != null) {
				try (CompileContext iScope = new CompileContext(scope)) {
					init.readCompile(RALExprSlice.concat(vars), iScope);
				}
			}
		}

		@Override
		public String toString() {
			return "let (...);";
		}
	}
}
