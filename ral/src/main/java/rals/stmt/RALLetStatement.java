/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.ScopeContext;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.expr.RALStringVar;
import rals.lex.SrcPos;
import rals.types.RALType;

/**
 * Let statement.
 * Introduces new locals or shadows existing locals.
 */
public class RALLetStatement extends RALStatement {
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
	protected void compileInner(StringBuilder writer, ScopeContext scope) {
		// Ok, so we want to define this local in the outer environment, but carefully.
		// In particular we want to be able to use local definitions as a cast.
		RALStringVar[] vars = new RALStringVar[names.length];
		for (int i = 0; i < vars.length; i++) {
			RALStringVar rsv = scope.allocLocal(types[i]);
			writer.append(" * ");
			writer.append(rsv.code);
			writer.append(": ");
			writer.append(types[i]);
			writer.append(" ");
			writer.append(names[i]);
			writer.append("\n");
			vars[i] = rsv;
		}

		if (init != null) {
			RALExpr re = init.resolve(scope);
			try (ScopeContext iScope = new ScopeContext(scope)) {
				RALType[] initChk = re.outTypes(iScope);
				if (initChk.length != names.length)
					throw new RuntimeException("Expression return values don't match amount of defined variables");
				for (int i = 0; i < vars.length; i++)
					initChk[i].implicitlyCastOrThrow(types[i]);
				re.outCompile(writer, vars, iScope);
			}
		}

		for (int i = 0; i < vars.length; i++)
			scope.scopedVariables.put(names[i], vars[i]);
	}
}
