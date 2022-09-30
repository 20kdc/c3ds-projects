/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.types.RALType;

/**
 * Underscore, or the discard expression.
 * This is injected in ScopeContext alongside other "semi-keywords".
 */
public class RALDiscard implements RALExpr {

	@Override
	public RALType[] outTypes(ScopeContext context) {
		return new RALType[0];
	}

	@Override
	public void outCompile(StringBuilder writer, RALExpr[] out, ScopeContext context) {
		// ?
	}

	@Override
	public void inCompile(StringBuilder writer, String[] input, RALType[] inputExactType, ScopeContext context) {
		// We need to discard this safely, soooo
		try (ScopeContext ic = new ScopeContext(context)) {
			RALStringVar rsv = context.allocLocal(input[0], inputExactType[0]);
			rsv.inCompile(writer, input, inputExactType, ic);
		}
	}

	@Override
	public RALType[] inTypes(ScopeContext context) {
		return new RALType[] {
			context.script.typeSystem.gAny
		};
	}
}
