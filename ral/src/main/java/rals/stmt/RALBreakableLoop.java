/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.lex.SrcPos;

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
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(StringBuilder writer, CompileContext context) {
				try (CompileContext ccs = new CompileContext(context)) {
					String labelTop = ccs.allocLabel();
					String labelEnd = ccs.allocLabel();
					ccs.clearBreak();
					ccs.breakLabel = labelEnd;
					writer.append("goto ");
					writer.append(labelTop);
					writer.append("\n");
					writer.append("subr ");
					writer.append(labelTop);
					writer.append("\n");
					rs.compile(writer, ccs);
					writer.append("goto ");
					writer.append(labelTop);
					writer.append("\n");
					writer.append("subr ");
					writer.append(labelEnd);
					writer.append("\n");
				}
			}
		};
	}
}
