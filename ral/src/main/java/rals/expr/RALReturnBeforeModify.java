/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.cctx.CompileContext;
import rals.code.ScopeContext;
import rals.stmt.RALStatement;
import rals.stmt.RALStatementUR;

/**
 * This is basically a reverse statement expression.
 * Not an inverse statement expression, a reverse statement expression.
 * (The distinction is shockingly important, due to inverse statement expressions being much more rigid type-wise)
 */
public class RALReturnBeforeModify implements RALExprUR {
	public final RALExprUR inner;
	public final RALStatementUR modify;
	public RALReturnBeforeModify(RALExprUR ie, RALStatementUR m) {
		inner = ie;
		modify = m;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final RALExprSlice innerRes = inner.resolve(scope);
		final RALStatement modifyRes = modify.resolve(scope);
		// Work out what to do about the typing
		final RALSlot[] slots = new RALSlot[innerRes.length];
		for (int i = 0; i < slots.length; i++) {
			RALSlot base = innerRes.slot(i);
			slots[i] = new RALSlot(base.type, base.perms.denyWrite());
		}
		return new RALExprSlice(slots.length) {
			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				try (CompileContext cc = context.forkVAEH()) {
					// We do the write early, and then do the modification.
					innerRes.readCompile(out, cc);
					modifyRes.compile(cc.writer, cc);
				}
			}

			@Override
			protected RALSlot slotInner(int index) {
				return slots[index];
			}
		};
	}
}
