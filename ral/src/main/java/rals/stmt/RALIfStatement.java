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
import rals.cond.RALCondition;
import rals.diag.SrcPos;
import rals.expr.RALConstant;
import rals.expr.RALExprUR;

/**
 * The first conditional.
 */
public class RALIfStatement extends RALStatementUR {
	public final RALExprUR condition;
	public final RALStatementUR mainBranch;
	public final RALStatementUR elseBranch;
	public final boolean invert;

	public RALIfStatement(SrcPos sp, RALExprUR c, RALStatementUR m, RALStatementUR e, boolean inv) {
		super(sp);
		condition = c;
		mainBranch = m;
		elseBranch = e;
		invert = inv;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		scope = new ScopeContext(scope);
		// scope juggling
		final RALConstant conditionConstant = condition.resolveConst(scope.world.types);
		ScopeContext subScope = new ScopeContext(scope);
		if (conditionConstant != null) {
			boolean answer = RALCondition.constToBool(conditionConstant);
			if (invert)
				answer = !answer;
			final RALStatementUR execBranch = answer ? mainBranch : elseBranch;
			// we don't really need another scope, since the condition had to be scopeless
			final RALStatement execBranchR = execBranch != null ? execBranch.resolve(subScope) : null;
			if (execBranchR != null)
				return execBranchR;
			return new RALStatement(extent) {
				@Override
				protected void compileInner(CodeWriter writer, CompileContext context) {
					// nothing to do here!
				}

				@Override
				public String toString() {
					return "if (DCE'd)";
				}
			};
		} else {
			final RALCondition conditionR = RALCondition.coerceToCondition(condition.resolve(subScope), scope.world.types);
			final RALStatement mainBranchR = mainBranch.resolve(new ScopeContext(subScope));
			final RALStatement elseBranchR = elseBranch != null ? elseBranch.resolve(new ScopeContext(subScope)) : null;
			return new RALStatement(extent) {
				@Override
				protected void compileInner(CodeWriter writer, CompileContext context) {
					try (CompileContext outerCtx = new CompileContext(context)) {
						String inl = conditionR.compileCond(writer, outerCtx, invert);
						writer.writeCode("doif " + inl, 1);
						try (CompileContext bsr = new CompileContext(outerCtx)) {
							mainBranchR.compile(writer, bsr);
						}
						if (elseBranchR != null) {
							writer.writeCode(-1, "else", 1);
							try (CompileContext bsr = new CompileContext(outerCtx)) {
								elseBranchR.compile(writer, bsr);
							}
						}
						writer.writeCode(-1, "endi");
					}
				}

				@Override
				public String toString() {
					return "if " + (invert ? "! " : "") + conditionR.toString() + " (...)";
				}
			};
		}
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
