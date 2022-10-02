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
 * INTENSIFYING SIGHING
 */
public class RALTarg extends RALVarBase implements RALExprUR {
	public RALTarg(RALType ot) {
		super(ot, true);
	}

	@Override
	public RALExprSlice resolve(ScopeContext scope) {
		return this;
	}

	@Override
	public String toString() {
		return "targ";
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		context.writer.writeCode("targ " + input);
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		out.writeCompile(0, "targ", type, context);
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContext context) {
		return RALSpecialInline.Targ;
	}
}
