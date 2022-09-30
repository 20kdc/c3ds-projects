/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;

/**
 * Unresolved expression. Created to ensure expressions get resolved.
 */
public interface RALExprUR {
	/**
	 * Must resolve.
	 * Note that ScopeContext can be null here, but doing that could lead to unwanted failures.
	 * So only do it if you're sure (read: const-related stuff)
	 */
	RALExpr resolve(ScopeContext context);
}
