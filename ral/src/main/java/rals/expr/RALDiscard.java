/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * Underscore, or the discard expression.
 * This is injected in ScopeContext alongside other "semi-keywords".
 */
public class RALDiscard implements RALExpr, RALExprUR {
	public static final RALDiscard INSTANCE = new RALDiscard();

	private RALDiscard() {
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		return this;
	}

	@Override
	public RALType[] outTypes(ScriptContext context) {
		return new RALType[0];
	}

	@Override
	public void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context) {
		// ?
	}

	@Override
	public void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context) {
		// We need to discard this safely, soooo
		try (ScopeContext ic = new ScopeContext(context)) {
			RALStringVar rsv = ic.allocLocal(input, inputExactType);
			rsv.inCompile(writer, input, inputExactType, context);
		}
	}

	@Override
	public RALType inType(ScriptContext context) {
		return context.typeSystem.gAny;
	}
}
