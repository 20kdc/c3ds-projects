/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.code.IVAHandle;
import rals.code.ScopeContext;
import rals.types.RALType;

/**
 * For trivial expressions & variables.
 * Goes nicely with inline statements.
 */
public class RALVAVar implements RALExpr {
	public final IVAHandle handle;
	public final RALType type;
	public RALVAVar(IVAHandle h, RALType ot) {
		handle = h;
		type = ot;
	}

	@Override
	public String toString() {
		return "VA[" + handle + "!" + type + "]";
	}

	@Override
	public RALType inType() {
		return type;
	}

	@Override
	public RALType[] outTypes() {
		return new RALType[] {
			type
		};
	}

	@Override
	public void inCompile(CodeWriter writer, String input, RALType inputExactType, CompileContext context) {
		RALStringVar.writeSet(writer, getInlineCAOS(context, true), input, inputExactType);
	}

	@Override
	public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
		out[0].inCompile(writer, getInlineCAOS(context, false), type, context);
	}

	@Override
	public String getInlineCAOS(CompileContext context, boolean write) {
		Integer i = context.heldVAHandles.get(handle);
		if (i == null)
			throw new RuntimeException("VA handle " + handle + " escaped containment");
		return ScopeContext.vaToString(i);
	}
}
