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
 * Underscore, or the discard expression.
 * This is injected in ScopeContext alongside other "semi-keywords".
 */
public class RALDiscard implements RALExpr, RALExprUR {
	public final RALType any;
	public RALDiscard(TypeSystem ts) {
		any = ts.gAny;
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		return this;
	}

	@Override
	public RALType[] outTypes() {
		return new RALType[0];
	}

	@Override
	public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
		throw new RuntimeException("Discard isn't a real value");
	}

	@Override
	public void inCompile(CodeWriter writer, String input, RALType inputExactType, CompileContext context) {
		// We need to discard this safely, soooo
		try (CompileContext ccr = new CompileContext(context)) {
			RALStringVar rsv = ccr.allocVA(inputExactType);
			rsv.inCompile(writer, input, inputExactType, context);
		}
	}

	@Override
	public RALType inType() {
		return any;
	}
}
