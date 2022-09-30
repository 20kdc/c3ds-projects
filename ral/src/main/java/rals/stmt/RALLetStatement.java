/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.expr.RALVAVar;
import rals.lex.SrcPos;
import rals.types.RALType;

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
	public RALStatement resolve(ScopeContext scope) {
		RALExpr initRes = null;
		if (init != null) {
			initRes = init.resolve(scope);
			// Type-check
			RALType[] initChk = initRes.outTypes();
			if (initChk.length != names.length)
				throw new RuntimeException("Expression return values don't match amount of defined variables");
			for (int i = 0; i < names.length; i++)
				initChk[i].implicitlyCastOrThrow(types[i]);
		}
		RALVAVar[] vars = new RALVAVar[names.length];
		for (int i = 0; i < names.length; i++) {
			RALVAVar rvv = scope.newLocal(names[i], types[i]);
			vars[i] = rvv;
		}
		return new Resolved(lineNumber, vars, initRes);
	}

	public class Resolved extends RALStatement {
		public final RALVAVar[] vars;
		public final RALExpr init;
	
		public Resolved(SrcPos sp, RALVAVar[] v, RALExpr i) {
			super(sp);
			vars = v;
			init = i;
		}
	
		@Override
		protected void compileInner(StringBuilder writer, CompileContext scope) {
			// Ok, so we want to define this local in the outer environment, but carefully.
			// In particular we want to be able to use local definitions as a cast.
			for (int i = 0; i < vars.length; i++) {
				scope.allocVA(vars[i].handle);
				writer.append(" * ");
				writer.append(vars[i].getInlineCAOS(scope));
				writer.append(": ");
				writer.append(types[i]);
				writer.append(" ");
				writer.append(names[i]);
				writer.append("\n");
			}
			
			if (init != null) {
				try (CompileContext iScope = new CompileContext(scope)) {
					init.outCompile(writer, vars, iScope);
				}
			}
		}
	}
}
