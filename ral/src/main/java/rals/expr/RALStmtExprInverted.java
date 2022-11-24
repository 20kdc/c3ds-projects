/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.stmt.*;

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
	public RALExprSlice resolveInner(ScopeContext scope) {
		// Notably, we don't need to do too much for this, just manipulate the context a bit.
		// However, we don't have our outputs yet, so we're going to need to fudge things.
		// Besides, it's healthy! Permissions checks and all that...
		scope = new ScopeContext(scope);
		final RALSlot[] types = new RALSlot[rets.length];
		final IEHHandle[] handles = new IEHHandle[rets.length];
		for (int i = 0; i < types.length; i++) {
			final MacroArg ret = rets[i];
			types[i] = new RALSlot(ret.type, RALSlot.Perm.R);
			final IEHHandle handle = new IEHHandle() {
				@Override
				public String toString() {
					return "macro ret-arg " + ret.name;
				}
			};
			handles[i] = handle;
			scope.scopedVariables.put(ret.name, new RALVarEH(handle, ret.type));
		}
		final RALStatement innards = code.resolve(scope);
		return new Resolved(types, innards, handles);
	}

	public static final class Resolved extends RALExprSlice {
		private final RALSlot[] slots;
		private final RALStatement innards;
		private final IEHHandle[] handles;

		public Resolved(RALSlot[] slots, RALStatement innards, IEHHandle[] handles) {
			super(slots.length);
			this.slots = slots;
			this.innards = innards;
			this.handles = handles;
		}

		@Override
		public String toString() {
			return "resolved StmtExprInverted";
		}

		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			// alright, now we're here, just need to wire this up
			try (CompileContext cci = new CompileContext(context)) {
				// These handles wire everything up nicely
				for (int i = 0; i < out.length; i++)
					cci.heldExprHandles.put(handles[i], out.slice(i, 1));
				innards.compile(context.writer, cci);
			}
		}

		@Override
		protected RALSlot slotInner(int index) {
			return slots[index];
		}
	}
}
