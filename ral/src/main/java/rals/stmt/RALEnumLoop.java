/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.expr.RALCast;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.lex.SrcPos;
import rals.types.RALType;

public class RALEnumLoop extends RALStatementUR {
	public final RALType targType;
	public final String enumToken;
	public final RALExprUR agent;
	public final RALStatementUR loopBody;

	public RALEnumLoop(SrcPos lineNumber, RALType iterOver, String subType, RALExprUR econAgent, RALStatementUR body) {
		super(lineNumber);
		targType = iterOver;
		enumToken = subType;
		agent = econAgent;
		loopBody = body;
	}

	@Override
	protected RALStatement resolveInner(ScopeContext scope) {
		scope = new ScopeContext(scope);
		final RALStatement loopStarter;
		if (agent != null) {
			loopStarter = new RALInlineStatement(lineNumber, new Object[] {
				enumToken + " ",
				RALCast.of(agent, scope.script.typeSystem.gAgent),
				"\n"
			}).resolve(scope);
		} else {
			if (!(targType instanceof RALType.AgentClassifier))
				throw new RuntimeException("Can't " + enumToken + " over " + targType + " as it's not an AgentClassifier");
			RALType.AgentClassifier ac = (RALType.AgentClassifier) targType;
			String code = enumToken + " " + ac.classifier.family + " " + ac.classifier.genus + " " + ac.classifier.species + "\n";
			loopStarter = new RALInlineStatement(lineNumber, new Object[] {
				code
			}).resolve(scope);
		}
		scope.scopedVariables.put("targ", RALCast.Resolved.of(scope.scopedVariables.get("targ"), targType, false));
		final boolean isAdjustingLoopBodyForBreak = true;
		final RALStatement loopBodyR = loopBody.resolve(scope);
		// finally make it
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(StringBuilder writer, CompileContext context) {
				try (CompileContext cc = new CompileContext(context)) {
					// just don't allow it
					cc.clearBreak();
					String endJumpLabel = cc.allocLabel();
					String breakBool = null;
					if (isAdjustingLoopBodyForBreak) {
						breakBool = cc.allocVA(cc.typeSystem.gBoolean).code;
						// initialize break bool to 0
						writer.append("setv ");
						writer.append(breakBool);
						writer.append(" 0\n");
					}
					loopStarter.compileInner(writer, cc);
					if (isAdjustingLoopBodyForBreak) {
						cc.breakLabel = endJumpLabel;
						cc.breakBool = breakBool;
						// if break bool is still 0, run body
						writer.append("doif ");
						writer.append(cc.breakBool);
						writer.append(" == 0\n");
					}
					loopBodyR.compileInner(writer, cc);
					if (isAdjustingLoopBodyForBreak) {
						// endif, then jump label NOP
						writer.append("endi\ngoto ");
						writer.append(endJumpLabel);
						writer.append("\nsubr ");
						writer.append(endJumpLabel);
						writer.append("\n");
					}
					writer.append("next\n");
				}
			}
		};
	}

}
