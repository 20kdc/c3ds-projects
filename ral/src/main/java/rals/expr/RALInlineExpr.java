/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.stmt.*;
import rals.types.RALType;

/**
 * Inline expression
 */
public class RALInlineExpr implements RALExprUR {
	public final Object[] parts;
	public final RALSlot.Perm perm;
	public RALInlineExpr(Object[] p, RALSlot.Perm perms) {
		parts = p;
		perm = perms;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final Object[] resolved = RALInlineStatement.resolveParts(parts, scope);
		final RALSlot slot = new RALSlot(scope.world.types.gAny, perm);
		return new Resolved(slot, resolved);
	}

	public static final class Resolved extends RALExprSlice {
		private final RALSlot slot;
		private final Object[] resolved;

		public Resolved(RALSlot slot, Object[] resolved) {
			super(1);
			this.slot = slot;
			this.resolved = resolved;
		}

		@Override
		protected RALSlot slotInner(int index) {
			return slot;
		}

		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			try (CompileContext c2 = new CompileContext(context)) {
				out.writeCompile(0, RALInlineStatement.compileResolvedParts(resolved, c2), RALType.Major.Unknown, c2);
			}
		}

		@Override
		protected void writeCompileInner(int index, String input, RALType.Major inputExactType, CompileContext context) {
			// having to manually check this is cringe, need to make RALExprSlice do these checks at some point
			// but I'm patching this midway through writing the bloody tutorial
			if (!slot.perms.write)
				throw new RuntimeException("Write to unwritable inline expression");
			try (CompileContext c2 = new CompileContext(context)) {
				RALVarString.writeSet(c2.writer, RALInlineStatement.compileResolvedParts(resolved, c2), input, inputExactType);
			}
		}

		@Override
		protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			if (write && !slot.perms.write)
				return null;
			return RALInlineStatement.compileResolvedParts(resolved, context);
		}
	}
}
