/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.stmt.*;
import rals.types.*;

/**
 * Inline expression
 */
public class RALInlineExpr implements RALExprUR {
	public final Object[] parts;
	public RALInlineExpr(Object[] p) {
		parts = p;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final Object[] resolved = RALInlineStatement.resolveParts(parts, scope);
		return new RALExprSlice(1) {
			@Override
			protected RALType readTypeInner(int index) {
				return scope.script.typeSystem.gAny;
			}
			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				try (CompileContext c2 = new CompileContext(context)) {
					out.writeCompile(0, RALInlineStatement.compileResolvedParts(resolved, c2), context.typeSystem.gAny, c2);
				}
			}
			@Override
			protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
				return RALInlineStatement.compileResolvedParts(resolved, context);
			}
		};
	}

}
