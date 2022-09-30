/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.ScopeContext;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.lex.SrcPos;

/**
 * Aliases an expression to another.
 */
public class RALAliasStatement extends RALStatement {
	public String name;
	public RALExprUR target;
	public RALAliasStatement(SrcPos sp, String id, RALExprUR e) {
		super(sp);
		name = id;
		target = e;
	}

	@Override
	protected void compileInner(StringBuilder writer, ScopeContext scope) {
		RALExpr exp = target.resolve(scope);
		writer.append(" * ");
		writer.append(exp);
		writer.append(": ");
		writer.append(name);
		writer.append("\n");
		scope.scopedVariables.put(name, exp);
	}
}
