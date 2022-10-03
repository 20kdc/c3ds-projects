/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.function.Consumer;

import rals.code.*;
import rals.types.*;

/**
 * Field access.
 */
public class RALFieldAccess implements RALExprUR {
	public final RALExprUR base;
	public final String field;

	public RALFieldAccess(RALExprUR b, String f) {
		base = b;
		field = f;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		final RALExprSlice baseExpr = base.resolve(scope);
		final RALType baseType = baseExpr.assert1ReadType().assertImpCast(scope.script.typeSystem.gAgent);
		final AgentInterface.OVar slot = baseType.lookupField(field);

		return new RALVarBase(slot.type, true) {

			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				String outInline = out.getInlineCAOS(0, true, context);
				if (outInline != null) {
					// This means we have a guarantee of being able to safely output, which is great
					inlineIO(context, (va) -> {
						RALVarString.writeSet(context.writer, outInline, va, slot.type);
					});
				} else {
					RALSpecialInline si = baseExpr.getSpecialInline(0, context);
					if (si == RALSpecialInline.Ownr) {
						// Don't do this with TARG because we could in theory be victim to a switcheroo.
						out.writeCompile(0, CompileContext.vaToString("mv", slot.slot), slot.type, context);
					} else {
						// alright, we're doing something complicated I see
						try (CompileContext cc = new CompileContext(context)) {
							// create a temporary, in which we'll store the result
							RALVarString.Fixed rsv = cc.allocVA(slot.type);
							String store = backupAndSet(cc);
							rsv.writeCompile(0, CompileContext.vaToString("ov", slot.slot), slot.type, context);
							restore(context.writer, store);
							// done, give it
							out.writeCompile(0, rsv.code, slot.type, context);
						}
					}
				}
			}

			@Override
			protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
				inlineIO(context, (va) -> {
					RALVarString.writeSet(context.writer, va, input, inputExactType);
				});
			}

			/**
			 * This is used for when we have a guarantee of being able to do whatever we needed to do in a simple CAOS line.
			 */
			private void inlineIO(CompileContext context, Consumer<String> doTheThing) {
				RALSpecialInline si = baseExpr.getSpecialInline(0, context);
				if ((si == RALSpecialInline.Ownr) || (si == RALSpecialInline.Targ)) {
					String pfx = "ov";
					if (si == RALSpecialInline.Ownr)
						pfx = "mv";
					doTheThing.accept(CompileContext.vaToString(pfx, slot.slot));
				} else {
					// alright, we're doing something complicated I see
					try (CompileContext cc = new CompileContext(context)) {
						String store = backupAndSet(cc);
						doTheThing.accept(CompileContext.vaToString("ov", slot.slot));
						restore(cc.writer, store);
					}
				}
			}

			private String backupAndSet(CompileContext cc) {
				String targTmpVA = CompileContext.vaToString(cc.allocVA());
				cc.writer.writeCode("seta " + targTmpVA + " targ");
				baseExpr.readCompile(new RALVarSI(RALSpecialInline.Targ, baseType, true), cc);
				return targTmpVA;
			}

			private void restore(CodeWriter writer, String targTmpVA) {
				writer.writeCode("targ " + targTmpVA);
			}

			@Override
			public String getInlineCAOSInner(int index, boolean write, CompileContext context) {
				// We can't trust that targ won't be messed with unless we ensure it personally.
				// That in mind, only translate this for OWNR.
				if (baseExpr.getSpecialInline(0, context) == RALSpecialInline.Ownr)
					return CompileContext.vaToString("mv", slot.slot);
				return null;
			}

			@Override
			public String toString() {
				return baseExpr + "[" + baseType + "." + field + "]";
			}
		};
	}

}
