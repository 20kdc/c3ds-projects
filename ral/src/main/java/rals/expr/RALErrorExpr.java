/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.CompileContextNW;
import rals.types.RALType;

/**
 * This expression represents an error.
 * It's useful for shuffling macro compile errors to time of use for ease of understanding.
 */
public final class RALErrorExpr extends RALExprSlice.ThickProxy {
	public final String errorText;
	public RALErrorExpr(String err) {
		super(0);
		errorText = err;
	}

	@Override
	public String toString() {
		return "err:" + this;
	}

	@Override
	protected RALSlot slotInner(int index) {
		throw new RuntimeException(errorText);
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		throw new RuntimeException(errorText);
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType.Major inputExactType, CompileContext context) {
		throw new RuntimeException(errorText);
	}

	@Override
	protected void readInplaceCompileInner(RALVarVA[] out, CompileContext context) {
		throw new RuntimeException(errorText);
	}

	@Override
	protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
		return null;
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
		return RALSpecialInline.None;
	}
}