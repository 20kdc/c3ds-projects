/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.io.StringWriter;
import java.util.function.Consumer;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.types.AgentInterface.OVar;
import rals.types.RALType;

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
	public RALExpr resolve(ScopeContext scope) {
		final RALExpr baseExpr = base.resolve(scope);
		final RALType baseType = baseExpr.assertOutTypeSingleImpcast(scope.script.typeSystem.gAgent);
		final OVar slot = baseType.lookupField(field);

		return new RALExpr() {
			@Override
			public RALType[] outTypes() {
				return new RALType[] {
					slot.type
				};
			}

			@Override
			public RALType inType() {
				return slot.type;
			}

			@Override
			public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
				String outInline = out[0].getInlineCAOS(context, true);
				if (outInline != null) {
					// This means we have a guarantee of being able to safely output, which is great
					inlineIO(writer, context, (va) -> {
						RALStringVar.writeSet(writer, outInline, va, slot.type);
					});
				} else {
					SpecialInline si = baseExpr.getSpecialInline(context);
					if (si == SpecialInline.Ownr) {
						// Don't do this with TARG because we could in theory be victim to a switcheroo.
						out[0].inCompile(writer, ScopeContext.vaToString("mv", slot.slot), slot.type, context);
					} else {
						// alright, we're doing something complicated I see
						try (CompileContext cc = new CompileContext(context)) {
							// create a temporary, in which we'll store the result
							RALStringVar rsv = cc.allocVA(slot.type);
							String store = backupAndSet(writer, cc);
							rsv.inCompile(writer, ScopeContext.vaToString("ov", slot.slot), slot.type, context);
							restore(writer, store);
							// done, give it
							out[0].inCompile(writer, rsv.code, slot.type, context);
						}
					}
				}
			}

			@Override
			public void inCompile(CodeWriter writer, String input, RALType inputExactType, CompileContext context) {
				inlineIO(writer, context, (va) -> {
					RALStringVar.writeSet(writer, va, input, inputExactType);
				});
			}

			/**
			 * This is used for when we have a guarantee of being able to do whatever we needed to do in a simple CAOS line.
			 */
			private void inlineIO(CodeWriter writer, CompileContext context, Consumer<String> doTheThing) {
				SpecialInline si = baseExpr.getSpecialInline(context);
				if ((si == SpecialInline.Ownr) || (si == SpecialInline.Targ)) {
					String pfx = "ov";
					if (si == SpecialInline.Ownr)
						pfx = "mv";
					doTheThing.accept(ScopeContext.vaToString(pfx, slot.slot));
				} else {
					// alright, we're doing something complicated I see
					try (CompileContext cc = new CompileContext(context)) {
						String store = backupAndSet(writer, cc);
						doTheThing.accept(ScopeContext.vaToString("ov", slot.slot));
						restore(writer, store);
					}
				}
			}

			private String backupAndSet(CodeWriter writer, CompileContext cc) {
				String targTmpVA = ScopeContext.vaToString(cc.allocVA());
				writer.writeCode("seta " + targTmpVA + " targ");
				baseExpr.outCompile(writer, new RALExpr[] {new RALSIVar(SpecialInline.Targ, baseType, true)}, cc);
				return targTmpVA;
			}

			private void restore(CodeWriter writer, String targTmpVA) {
				writer.writeCode("targ " + targTmpVA);
			}

			@Override
			public String getInlineCAOS(CompileContext context, boolean write) {
				// We can't trust that targ won't be messed with unless we ensure it personally.
				// That in mind, only translate this for OWNR.
				if (baseExpr.getSpecialInline(context) == SpecialInline.Ownr)
					return ScopeContext.vaToString("mv", slot.slot);
				return null;
			}
		};
	}

}
