/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.lex.DefInfo;

/**
 * Something callable (i.e. a macro or something like it)...
 */
public interface RALCallable {
	/**
	 * Runs any pre-compilation required. 
	 */
	void precompile(UnresolvedWorld world);

	/**
	 * Given some arguments, converts to an expression.
	 * See RALExpr.resolve for details on how this all works. 
	 */
	RALExprSlice instance(RALExprSlice args, ScopeContext sc);

	/**
	 * Gets definition info, or at least one instance of such.
	 */
	DefInfo getDefInfo();
}
