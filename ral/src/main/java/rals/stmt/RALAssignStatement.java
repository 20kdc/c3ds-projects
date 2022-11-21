/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.diag.SrcPos;
import rals.expr.*;
import rals.lex.*;

/**
 * Assignment statement
 */
public class RALAssignStatement extends RALStatementUR {
	public final RALExprUR targets;
	public final RALExprUR source;

	public RALAssignStatement(SrcPos ln, RALExprUR a, RALExprUR b) {
		super(ln);
		targets = a;
		source = b;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		// Break scope here so we don't leak temporaries.
		scope = new ScopeContext(scope);
		final RALExprSlice targetsR = targets == null ? null : targets.resolve(scope);
		final RALExprSlice sourceR = source.resolve(scope);
		if (targets != null) {
			// Type-check
			if (targetsR.length != sourceR.length)
				throw new RuntimeException("Targets len " + targetsR.length + " != sources len " + sourceR.length);
			for (int i = 0; i < targetsR.length; i++)
				sourceR.readType(i).implicitlyCastOrThrow(targetsR.writeType(i), sourceR, targetsR);
		}
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext cc) {
				// Break scope here so we don't leak temporaries.
				try (CompileContext c2 = new CompileContext(cc)) {
					if (targetsR == null) {
						// Assign everything to discard
						sourceR.readCompile(new RALDiscard(c2.typeSystem, sourceR.length), c2);
					} else {
						sourceR.readCompile(targetsR, c2);
					}
				}
			}

			@Override
			public String toString() {
				return targetsR + " = " + sourceR;
			}
		};
	}
}
