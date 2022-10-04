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
 * For trivial expressions & variables.
 * Goes nicely with inline statements.
 */
public class RALVarVA extends RALVarString {
	public final IVAHandle handle;
	public RALVarVA(IVAHandle h, RALType ot) {
		super(ot, true);
		handle = h;
	}

	@Override
	public String toString() {
		return "VA[" + handle + "!" + type + "]";
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		// OPT: As a VA, we do not have side effects on reads.
		// So if we're going to be writing to Discard, drop the matter immediately.
		if (out.getSpecialInline(0, context) == RALSpecialInline.Discard)
			return;
		// Continue with normal procedure.
		super.readCompileInner(out, context);
	}

	@Override
	public String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
		if (write && !isWritable)
			return null;
		Integer i = context.heldVAHandles.get(handle);
		if (i == null)
			throw new RuntimeException("VA handle " + handle + " escaped containment");
		return CompileContext.vaToString(i);
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
		return RALSpecialInline.VA;
	}
}
