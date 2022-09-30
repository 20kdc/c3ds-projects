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
 * Expression. Outputs a given set of types.
 */
public interface RALExpr {
	/**
	 * In the given context, what types can be read?
	 */
	RALType[] outTypes(ScriptContext context);

	/**
	 * Compiles this expression.
	 */
	void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context);

	/**
	 * In the given context, what type can be written?
	 */
	RALType inType(ScriptContext context);

	/**
	 * Compiles a write.
	 * WARNING: May alter TARG before input runs. If this matters, make a temporary.
	 */
	void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context);

	/**
	 * Gets the inline CAOS for this expression, or null if that's not possible.
	 * This acts as a "fast-path" to avoid temporary variables.
	 * It's also critical to how inline statements let you modify variables, hence the name.
	 */
	default String getInlineCAOS() {
		return null;
	}
}
