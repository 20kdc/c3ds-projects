/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.ScopeContext;
import rals.expr.RALDiscard;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.lex.SrcPos;
import rals.types.RALType;

/**
 * Assignment statement
 */
public class RALAssignStatement extends RALStatement {
	public final RALExprUR[] targets;
	public final RALExprUR source;

	public RALAssignStatement(SrcPos ln, RALExprUR[] a, RALExprUR b) {
		super(ln);
		targets = a;
		source = b;
	}

	@Override
	protected void compileInner(StringBuilder writer, ScopeContext scope) {
		if (targets == null) {
			// Assign everything to discard
			RALExpr be = source.resolve(scope);
			RALType[] sourceTypes = be.outTypes(scope.script);
			RALDiscard rd = RALDiscard.INSTANCE;
			RALExpr[] discards = new RALExpr[sourceTypes.length];
			for (int i = 0; i < discards.length; i++)
				discards[i] = rd;
			be.outCompile(writer, discards, scope.script);
		} else {
			RALExpr[] ae = new RALExpr[targets.length];
			RALType[] targetTypes = new RALType[targets.length];
			for (int i = 0; i < ae.length; i++) {
				ae[i] = targets[i].resolve(scope);
				targetTypes[i] = ae[i].inType(scope.script);
			}
			RALExpr be = source.resolve(scope);
			RALType[] sourceTypes = be.outTypes(scope.script);
	
			// type-check
			if (targetTypes.length != sourceTypes.length)
				throw new RuntimeException("Targets != sources");
			for (int i = 0; i < targetTypes.length; i++)
				sourceTypes[i].implicitlyCastOrThrow(targetTypes[i]);
			
			be.outCompile(writer, ae, scope.script);
		}
	}
}
