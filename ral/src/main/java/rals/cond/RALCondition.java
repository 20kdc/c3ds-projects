/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cond;

import rals.code.ScopeContext;

/**
 * Condition.
 */
public interface RALCondition {
	/**
	 * Compiles a condition. The CAOS condition code is returned.
	 * writer writes into the prelude.
	 * sharedScopeContext is a context held between the prelude and the use of the condition.
	 */
	String compile(StringBuilder writer, ScopeContext sharedScopeContext);
}
