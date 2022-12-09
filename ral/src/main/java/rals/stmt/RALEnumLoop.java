/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.cctx.*;
import rals.code.*;
import rals.diag.SrcPos;
import rals.expr.*;
import rals.types.*;

public class RALEnumLoop extends RALStatementUR {
	public final RALType targType;
	public final String enumToken;
	public final RALExprUR params;
	public final RALStatementUR loopBody;

	public RALEnumLoop(SrcPos lineNumber, RALType iterOver, String subType, RALExprUR p, RALStatementUR body) {
		super(lineNumber);
		targType = iterOver;
		enumToken = subType;
		params = p;
		loopBody = body;
	}

	@Override
	protected RALStatement resolveInner(ScopeContext outerScope) {
		ScopeContext scope = new ScopeContext(outerScope);
		RALExprSlice paramsR = params.resolve(scope);
		final RALStatement loopStarter;
		if (enumToken.equals("econ")) {
			paramsR.assert1ReadType().assertImpCast(scope.world.types.gAgent);
			loopStarter = new RALInlineStatement.Resolved(extent, new Object[] {
				enumToken + " ",
				paramsR
			});
		} else if (enumToken.equals("enum") || enumToken.equals("epas") || enumToken.equals("esee") || enumToken.equals("etch")) {
			if (paramsR.length == 0) {
				if (!(targType instanceof RALType.AgentClassifier))
					throw new RuntimeException("Can't " + enumToken + " over " + targType + " as it's not an AgentClassifier");
				RALType.AgentClassifier ac = (RALType.AgentClassifier) targType;
				loopStarter = new RALInlineStatement.Resolved(extent, new Object[] {
					enumToken + " " + ac.classifier.family + " " + ac.classifier.genus + " " + ac.classifier.species
				});
			} else if (paramsR.length == 3) {
				paramsR.readType(0).assertImpCast(scope.world.types.gInteger);
				paramsR.readType(1).assertImpCast(scope.world.types.gInteger);
				paramsR.readType(2).assertImpCast(scope.world.types.gInteger);
				loopStarter = new RALInlineStatement.Resolved(extent, new Object[] {
					enumToken + " ",
					paramsR
				});
			} else {
				throw new RuntimeException(enumToken + " can either have no parameters (classifier from targ-cast) or 3 (classifier from values)");
			}
		} else {
			throw new RuntimeException("Unrecognized subtype " + enumToken);
		}

		// Declare the new targ type
		scope.regenerateTarg(lineNumber, targType);

		final boolean isAdjustingLoopBodyForBreak = true;
		final RALStatement loopBodyR = loopBody.resolve(scope);
		// targ's type changes to that of Agent?
		outerScope.regenerateTarg(lineNumber, scope.world.types.gAgentNullable);

		// finally make it
		return new RALStatement(extent) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext context) {
				try (CompileContext ccVAEH = context.forkVAEH()) {
					// just don't allow it
					String endJumpLabel = ccVAEH.allocLabel();
					String breakBool = null;
					if (isAdjustingLoopBodyForBreak) {
						breakBool = ccVAEH.allocVA(ccVAEH.typeSystem.gBoolean, "RALEnumLoop break boolean").getCode(ccVAEH);
						// initialize break bool to 0
						writer.writeCode("setv " + breakBool + " 0");
					}
					String breakLabel = isAdjustingLoopBodyForBreak ? endJumpLabel : null;
					try (CompileContext cc = ccVAEH.forkBreak(breakLabel, breakBool)) {
						loopStarter.compileInner(writer, cc);
						// loopStarter is weird, do indent manually
						writer.indent++;
						if (isAdjustingLoopBodyForBreak) {
							// if break bool is still 0, run body
							writer.writeCode("doif " + breakBool + " eq 0");
						}
						loopBodyR.compileInner(writer, cc);
						if (isAdjustingLoopBodyForBreak) {
							// endif, then jump label NOP
							writer.writeCode("endi");
							writer.writeCode("goto " + endJumpLabel);
							writer.writeCode("subr " + endJumpLabel);
						}
						writer.writeCode(-1, "next");
					}
				}
			}
			@Override
			public String toString() {
				return enumToken + " " + targType;
			}
		};
	}

}
