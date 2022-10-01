/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.IEHHandle;
import rals.types.RALType;

/**
 * Used by the inverted statement expression stuff.
 */
public class RALEHVar implements RALExpr {
	public final IEHHandle handle;
	public final RALType type;
	public RALEHVar(IEHHandle h, RALType ot) {
		handle = h;
		type = ot;
	}

	@Override
	public String toString() {
		return "EH[" + handle + "!" + type + "]";
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

	public RALExpr getUnderlying(CompileContext cc) {
		RALExpr ex = cc.heldExprHandles.get(handle);
		if (ex == null)
			throw new RuntimeException("Missing: " + this);
		return ex;
	}

	@Override
	public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
		getUnderlying(context).inCompile(writer, input, inputExactType, context);
	}

	@Override
	public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
		getUnderlying(context).outCompile(writer, out, context);
	}

	@Override
	public String getInlineCAOS(CompileContext context) {
		return getUnderlying(context).getInlineCAOS(context);
	}

	@Override
	public SpecialInline getSpecialInline(CompileContext context) {
		return getUnderlying(context).getSpecialInline(context);
	}
}
