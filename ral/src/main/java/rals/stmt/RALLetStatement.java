/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.expr.*;
import rals.lex.*;
import rals.types.*;

/**
 * Let statement.
 * Introduces new locals or shadows existing locals.
 */
public class RALLetStatement extends RALStatementUR {
	public final String[] names;
	public final RALType[] types;
	public final RALExprUR init;

	public RALLetStatement(SrcPos sp, String[] n, RALType[] t, RALExprUR i) {
		super(sp);
		names = n;
		types = t;
		init = i;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		RALExprSlice initRes = null;
		if (init != null) {
			initRes = init.resolve(scope);
			// Type-check
			if (initRes.length != names.length)
				throw new RuntimeException("Expression return values don't match amount of defined variables");
			for (int i = 0; i < names.length; i++)
				initRes.readType(i).assertImpCast(types[i]);
		}
		RALVarVA[] vars = new RALVarVA[names.length];
		for (int i = 0; i < names.length; i++) {
			RALVarVA rvv = scope.newLocal(names[i], types[i]);
			vars[i] = rvv;
		}
		return new Resolved(lineNumber, vars, initRes);
	}

	public class Resolved extends RALStatement {
		public final RALVarVA[] vars;
		public final RALExprSlice init;
	
		public Resolved(SrcPos sp, RALVarVA[] v, RALExprSlice i) {
			super(sp);
			vars = v;
			init = i;
		}
	
		@Override
		protected void compileInner(CodeWriter writer, CompileContext scope) {
			// Ok, so we want to define this local in the outer environment, but carefully.
			// In particular we want to be able to use local definitions as a cast.
			for (int i = 0; i < vars.length; i++) {
				scope.allocVA(vars[i].handle);
				writer.writeComment(vars[i].getInlineCAOS(0, false, scope) + ": " + types[i] + " " + names[i]);
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
