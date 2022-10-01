/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.io.StringWriter;

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
			public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
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

			@Override
			public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
				SpecialInline si = baseExpr.getSpecialInline(context);
				if ((si == SpecialInline.Ownr) || (si == SpecialInline.Targ)) {
					String pfx = "ov";
					if (si == SpecialInline.Ownr)
						pfx = "mv";
					RALStringVar.writeSet(writer, ScopeContext.vaToString(pfx, slot.slot), input, inputExactType);
				} else {
					// alright, we're doing something complicated I see
					try (CompileContext cc = new CompileContext(context)) {
						String store = backupAndSet(writer, cc);
						// Do the thing
						RALStringVar.writeSet(writer, ScopeContext.vaToString("ov", slot.slot), input, inputExactType);
						restore(writer, store);
					}
				}
			}

			private String backupAndSet(StringBuilder writer, CompileContext cc) {
				String targTmpVA = ScopeContext.vaToString(cc.allocVA());
				writer.append("seta ");
				writer.append(targTmpVA);
				writer.append(" targ\n");
				baseExpr.outCompile(writer, new RALExpr[] {new RALSIVar(SpecialInline.Targ, baseType, true)}, cc);
				return targTmpVA;
			}

			private void restore(StringBuilder writer, String targTmpVA) {
				writer.append("seta targ ");
				writer.append(targTmpVA);
				writer.append("\n");
			}

			@Override
			public String getInlineCAOS(CompileContext context) {
				// We can't trust that targ won't be messed with unless we ensure it personally.
				// That in mind, only translate this for OWNR.
				if (baseExpr.getSpecialInline(context) == SpecialInline.Ownr)
					return ScopeContext.vaToString("mv", slot.slot);
				return null;
			}
		};
	}

}
