/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.cond.RALCondition;
import rals.expr.RALExprUR;
import rals.lex.SrcPos;

/**
 * The first conditional.
 */
public class RALIfStatement extends RALStatementUR {
	public final RALExprUR condition;
	public final RALStatementUR mainBranch;
	public final RALStatementUR elseBranch;

	public RALIfStatement(SrcPos sp, RALExprUR c, RALStatementUR m, RALStatementUR e) {
		super(sp);
		condition = c;
		mainBranch = m;
		elseBranch = e;
	}

	@Override
	public RALStatement resolve(ScopeContext scope) {
		scope = new ScopeContext(scope);
		// deliberately run the condition in the same scope as the contents
		// this might turn out to be useful
		ScopeContext subScope = new ScopeContext(scope);
		final RALCondition conditionR = RALCondition.coerceToCondition(condition.resolve(subScope), scope.script.typeSystem);
		final RALStatement mainBranchR = mainBranch.resolve(subScope);
		final RALStatement elseBranchR = elseBranch != null ? elseBranch.resolve(new ScopeContext(scope)) : null;
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(StringBuilder writer, CompileContext context) {
				try (CompileContext bsr = new CompileContext(context)) {
					String inl = conditionR.compileCond(writer, bsr);
					writer.append("doif ");
					writer.append(inl);
					writer.append("\n");
					mainBranchR.compile(writer, bsr);
				}
				if (elseBranchR != null) {
					writer.append("else\n");
					try (CompileContext bsr = new CompileContext(context)) {
						elseBranchR.compile(writer, bsr);
					}
				}
				writer.append("endi\n");
			}
		};
	}

	public static class Branch<C, T> {
		public final C condition;
		public final T code;
		public Branch(C c, T t) {
			condition = c;
			code = t;
		}
	}
}
