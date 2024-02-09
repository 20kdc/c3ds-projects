/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.cctx.*;
import rals.code.*;
import rals.types.*;

/**
 * INTENSIFYING SIGHING
 */
public class RALVarTarg extends RALVarBase implements RALExprUR {
	public RALVarTarg(RALType ot) {
		super(ot, true);
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		return this;
	}

	@Override
	public String toString() {
		// [CAOS]
		return "targ";
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType.Major inputExactType, CompileContext context) {
		context.writer.writeCode("targ " + input);
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		out.writeCompile(0, "targ", type.majorType, context);
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
		return RALSpecialInline.Targ;
	}
}
