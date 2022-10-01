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
 * This is EVEN MORE WRONG!
 */
public class RALBreakFromLoop extends RALStatementUR {
	public RALBreakFromLoop(SrcPos sp) {
		super(sp);
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(StringBuilder writer, CompileContext context) {
				if (context.breakBool != null) {
					writer.append("setv ");
					writer.append(context.breakBool);
					writer.append(" 1\n");
				}
				if (context.breakLabel != null) {
					writer.append("goto ");
					writer.append(context.breakLabel);
					writer.append("\n");
				} else {
					throw new RuntimeException("Cannot break at this place!");
				}
			}
		};
	}

}
