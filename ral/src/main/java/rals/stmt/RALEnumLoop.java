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

/**
 * Dear future people who need to know what the hell I'm doing with breaking out of enum loops:
 * PLEASE READ THIS FILE.
 * Specifically inside the break handler.
 */
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

		final RALStatement loopBodyR = loopBody.resolve(scope);
		// targ's type changes to that of Agent?
		outerScope.regenerateTarg(lineNumber, scope.world.types.gAgentNullable);

		// finally make it
		return new RALStatement(extent) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext context) {
				// We need two break contexts in a sandwich because of the whole "global break/continue label handles" thing
				// We don't want to overwrite the outer break label handle
				// But we need to ensure the escape occurs
				try (CompileContext ccOuter = context.forkVAEHBreak(IBreakHandler.NOP)) {
					// just don't allow it
					String endJumpLabel = ccOuter.allocLabel(ILabelHandle.BREAK);
					try (CompileContext cc = ccOuter.forkBreak(EnumBreaker.INSTANCE)) {
						String continueLabel = cc.allocLabel(ILabelHandle.CONTINUE);
						loopStarter.compileInner(writer, cc);
						// loopStarter is weird, do indent manually
						writer.indent++;
						loopBodyR.compileInner(writer, cc);
						if (cc.isLabelDefinedHereUsed(ILabelHandle.CONTINUE)) {
							writer.writeCode("goto " + continueLabel);
							writer.writeCode("subr " + continueLabel);
						}
						writer.writeCode(-1, "next");
					}
					if (ccOuter.isLabelDefinedHereUsed(ILabelHandle.BREAK)) {
						writer.writeCode("goto " + endJumpLabel);
						writer.writeCode("subr " + endJumpLabel);
					}
				}
			}
			@Override
			public String toString() {
				return enumToken + " " + targType;
			}
		};
	}

	public static final class EnumBreaker implements IBreakHandler {
		public static final EnumBreaker INSTANCE = new EnumBreaker();
		@Override
		public void compile(CompileContext context) {
			// we want to write all of this as a single line for sanity's sake
			// "you are not expected to understand this" moment
			StringBuilder sb = new StringBuilder();
			// allocate our labels
			String untlPop = context.allocLabel();
			String flyingNext = context.allocLabel();
			// this actually enters the break code
			// control flow here is a bit... complex
			sb.append("goto " + untlPop);
			sb.append(" enum 0 0 0");
			{
				sb.append(" subr " + flyingNext);
			}
			// this is the "flying next"
			// we can't reuse the same next because subr acts as STOP, so we need a flying one
			// (and we want our inner loop as small as possible, so no GOTO there)
			sb.append(" next");
			// however, the "flying next" can IMMEDIATELY return if the agent we were on was the last
			// so need to skip over the body of this if that happens
			sb.append(" doif 1 = 0");
			{
				sb.append(" enum 0 0 0");
				{
					sb.append(" loop");
					{
						sb.append(" subr " + untlPop);
					}
					// use UNTL to pop the original loop start address
					sb.append(" untl 0 = 0");
					// then use GSUB to put the new address (just before NEXT) on stack
					// flyingNext then runs the NEXT to take us back here
					// (alternatively it stops immediately and skips the doif)
					sb.append(" gsub " + flyingNext);
				}
				// <- loop start address
				sb.append(" next");
			}
			sb.append(" endi");
			// we're out!
			context.writer.writeComment("leave ENUM");
			context.writer.writeCode(sb.toString());
		}
	}
}
