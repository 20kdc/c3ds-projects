/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.cctx.*;
import rals.code.ScopeContext;
import rals.diag.SrcPos;

/**
 * Oh, now this is just WRONG.
 */
public class RALBreakableLoop extends RALStatementUR {
	public final RALStatementUR content;
	public RALBreakableLoop(SrcPos sp, RALStatementUR c) {
		super(sp);
		content = c;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		final RALStatement rs = content.resolve(new ScopeContext(scope));
		return new RALStatement(extent) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext context) {
				String labelTop = context.allocLabel();
				String labelEnd = context.allocLabel();
				try (CompileContext ccs = context.forkVAEHBreak(labelEnd, null)) {
					writer.writeCode("goto " + labelTop);
					writer.writeCode("subr " + labelTop, 1);
					rs.compile(writer, ccs);
					writer.writeCode(-1, "goto " + labelTop);
					writer.writeCode("subr " + labelEnd);
				}
			}
			@Override
			public String toString() {
				return "loop { (...) }";
			}
		};
	}
}
