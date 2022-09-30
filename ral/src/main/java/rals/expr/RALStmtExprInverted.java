/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.IEHHandle;
import rals.code.IVAHandle;
import rals.code.MacroArg;
import rals.code.ScopeContext;
import rals.stmt.RALStatement;
import rals.stmt.RALStatementUR;
import rals.types.RALType;

/**
 * Same basic idea as RALStmtExpr, but done differently.
 * Instead of making statements return something, the return expressions are passed in!
 */
public class RALStmtExprInverted implements RALExprUR {
	public final MacroArg[] rets;
	public final RALStatementUR code;

	public RALStmtExprInverted(MacroArg[] a, RALStatementUR rs) {
		rets = a;
		code = rs;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		// Notably, we don't need to do too much for this, just manipulate the context a bit.
		// However, we don't have our outputs yet, so we're going to need to fudge things.
		// Besides, it's healthy! Permissions checks and all that...
		scope = new ScopeContext(scope);
		final RALType[] types = new RALType[rets.length];
		final IEHHandle[] handles = new IEHHandle[rets.length];
		for (int i = 0; i < types.length; i++) {
			final MacroArg ret = rets[i];
			types[i] = ret.type;
			final IEHHandle handle = new IEHHandle() {
				@Override
				public String toString() {
					return "macro retarg " + ret.name;
				}
			};
			handles[i] = handle;
			scope.scopedVariables.put(ret.name, new RALEHVar(handle, ret.type));
		}
		final RALStatement innards = code.resolve(scope);
		return new RALExpr() {
			@Override
			public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
				throw new RuntimeException("Can't put values into StmtExprInverted (statement macro)");
			}

			@Override
			public RALType inType() {
				throw new RuntimeException("Can't put values into StmtExprInverted (statement macro)");
			}

			@Override
			public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
				// alright, now we're here, just need to wire this up
				try (CompileContext cci = new CompileContext(context)) {
					// These handles wire everything up nicely
					for (int i = 0; i < out.length; i++)
						cci.heldExprHandles.put(handles[i], out[i]);
					innards.compile(writer, cci);
				}
			}

			@Override
			public RALType[] outTypes() {
				return types;
			}
		};
	}

}
