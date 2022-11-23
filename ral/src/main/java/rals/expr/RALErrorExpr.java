/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.types.RALType;

/**
 * This expression represents an error.
 * It's useful for shuffling macro compile errors to time of use for ease of understanding.
 */
public final class RALErrorExpr extends RALExprSlice {
	public final String errorText;
	public RALErrorExpr(String err) {
		super(0);
		errorText = err;
	}

	@Override
	protected RALType readTypeInner(int index) {
		throw new RuntimeException(errorText);
	}

	@Override
	protected RALType writeTypeInner(int index) {
		return readTypeInner(0);
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		readTypeInner(0);
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		readTypeInner(0);
	}
}