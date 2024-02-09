/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.function.Consumer;

import rals.caos.CAOSUtils;
import rals.cctx.*;
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
		final RALType baseType = baseExpr.assert1ReadType().assertImpCast(scope.world.types.gAgent);
		final AgentInterface.OVar slot = baseType.lookupField(field);
		if (slot == null)
			throw new RuntimeException("Unknown field " + baseType + "." + field);
		return new RALVarBase(slot.type, true) {

			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				String outInline = out.getInlineCAOS(0, true, context);
				if (outInline != null) {
					// This means we have a guarantee of being able to safely output, which is great
					inlineIO(context, (va) -> {
						RALVarString.writeSet(context.writer, outInline, va, slot.type.majorType.autoPromote(out.slot(0).type.majorType));
					});
				} else {
					RALSpecialInline si = baseExpr.getSpecialInline(0, context);
					if (si == RALSpecialInline.Ownr) {
						// Don't do this with TARG because we could in theory be victim to a switcheroo.
						// [CAOS]
						String vaInline = context.typeSystem.codeGenFeatureLevel.hasMVXX ? CAOSUtils.vaToString("mv", slot.slot) : ("avar ownr " + slot.slot);
						out.writeCompile(0, vaInline, slot.type.majorType, context);
					} else {
						// use fallbackIO - this stores the agent reference in a temporary before we do the writeCompile
						// as such, it's immune to targ switcheroo
						fallbackIO(context, (va) -> {
							out.writeCompile(0, va, slot.type.majorType, context);
						});
					}
				}
			}

			@Override
			protected void writeCompileInner(int index, String input, RALType.Major inputExactType, CompileContext context) {
				inlineIO(context, (va) -> {
					RALVarString.writeSet(context.writer, va, input, inputExactType.autoPromote(slot.type.majorType));
				});
			}

			/**
			 * This is used for when out.writeCompile is not involved
			 */
			private void inlineIO(CompileContext context, Consumer<String> doTheThing) {
				// try fast-path inline
				String fullInline = getInlineCAOSInner(0, true, context);
				if (fullInline != null) {
					doTheThing.accept(fullInline);
					return;
				}
				fallbackIO(context, doTheThing);
			}

			/**
			 * This is used when out.writeCompile is involved or if we can't do inline anyway
			 */
			private void fallbackIO(CompileContext context, Consumer<String> doTheThing) {
				// fast-path inline failed - we need to use a temporary to hold the agent reference
				try (CompileContext cc = context.forkVAEH()) {
					RALVarVA va = cc.allocVA(baseType, "RALFieldAccess computed agent");
					baseExpr.readInplaceCompile(new RALVarVA[] {va}, cc);
					// [CAOS]
					doTheThing.accept("avar " + va.getCode(cc) + " " + slot.slot);
				}
			}

			@Override
			public String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
				RALSpecialInline si = baseExpr.getSpecialInline(0, context);
				// [CAOS]
				if (si == RALSpecialInline.Ownr) {
					if (context.typeSystem.codeGenFeatureLevel.hasMVXX) {
						// obvious fast-path for ownr variables on DS
						return CAOSUtils.vaToString("mv", slot.slot);
					} else {
						// fall back to avar for CA/C3
						return "avar ownr " + slot.slot;
					}
				} else if (si == RALSpecialInline.Targ) {
					// obvious fast-path for targ variables
					return CAOSUtils.vaToString("ov", slot.slot);
				} else {
					// if we can inline the agent reference, we're fine
					String agentRefInline = baseExpr.getInlineCAOS(0, false, context);
					// [CAOS]
					if (agentRefInline != null)
						return "avar " + agentRefInline + " " + slot.slot;
				}
				return null;
			}

			@Override
			public String toString() {
				return baseExpr + "[" + baseType + "." + field + "]";
			}
		};
	}

}
