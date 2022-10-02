/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.types.*;

/**
 * Used by the inverted statement expression stuff.
 */
public class RALVarEH extends RALExprSlice {
	public final IEHHandle handle;
	public final RALType type;

	public RALVarEH(IEHHandle h, RALType ot) {
		super(1);
		handle = h;
		type = ot;
	}

	@Override
	public String toString() {
		return "EH[" + handle + "!" + type + "]";
	}

	@Override
	protected RALType readTypeInner(int index) {
		return type;
	}

	@Override
	protected RALType writeTypeInner(int index) {
		// Yes, this is unchecked.
		// This is the reason I'm so paranoid about making sure compile functions throw.
		return type;
	}

	public RALExprSlice getUnderlying(CompileContext cc) {
		RALExprSlice ex = cc.heldExprHandles.get(handle);
		if (ex == null)
			throw new RuntimeException("Missing: " + this);
		return ex;
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		getUnderlying(context).readCompile(out, context);
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		getUnderlying(context).writeCompile(index, input, inputExactType, context);
	}

	@Override
	protected String getInlineCAOSInner(int index, boolean write, CompileContext context) {
		return getUnderlying(context).getInlineCAOS(index, write, context);
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContext context) {
		return getUnderlying(context).getSpecialInlineInner(index, context);
	}
}
