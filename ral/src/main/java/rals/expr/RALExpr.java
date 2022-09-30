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
 * Expression. Outputs a given set of types.
 */
public interface RALExpr extends RALExprUR {

	@Override
	default RALExpr resolve(ScopeContext context) {
		return this;
	}

	/**
	 * In the given context, what types are here?
	 */
	RALType[] outTypes(ScopeContext context);

	/**
	 * Compiles this expression.
	 */
	void outCompile(StringBuilder writer, RALExpr[] out, ScopeContext context);

	/**
	 * In the given context, what types are here?
	 */
	RALType[] inTypes(ScopeContext context);

	/**
	 * Compiles a RALWritable.
	 * WARNING: May alter TARG before input runs. If this matters, make a temporary.
	 */
	void inCompile(StringBuilder writer, String[] input, RALType[] inputExactType, ScopeContext context);
}
