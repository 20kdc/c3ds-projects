/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.expr.RALDiscard;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.lex.SrcPos;
import rals.types.RALType;

/**
 * Assignment statement
 */
public class RALAssignStatement extends RALStatementUR {
	public final RALExprUR[] targets;
	public final RALExprUR source;

	public RALAssignStatement(SrcPos ln, RALExprUR[] a, RALExprUR b) {
		super(ln);
		targets = a;
		source = b;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		final RALExpr[] targetsR = targets == null ? null : resolveExprs(targets, scope);
		final RALType[] targetsT = targets == null ? null : inTypesOf(targetsR);
		final RALExpr sourceR = source.resolve(scope);
		if (targets != null) {
			// Type-check
			RALType[] sourceTypes = sourceR.outTypes();
			if (targetsT.length != sourceTypes.length)
				throw new RuntimeException("Targets len != sources len");
			for (int i = 0; i < targetsT.length; i++)
				sourceTypes[i].implicitlyCastOrThrow(targetsT[i], sourceR, targetsR[i]);
		}
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext cc) {
				if (targets == null) {
					// Assign everything to discard
					RALExpr[] discards = new RALExpr[sourceR.outTypes().length];
					for (int i = 0; i < discards.length; i++)
						discards[i] = new RALDiscard(cc.typeSystem);
					sourceR.outCompile(writer, discards, cc);
				} else {
					sourceR.outCompile(writer, targetsR, cc);
				}
			}

			@Override
			public String toString() {
				return "(...) = " + sourceR;
			}
		};
	}
}
