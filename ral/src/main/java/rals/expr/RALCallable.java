/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

/**
 * Something callable (i.e. a macro or something like it)...
 */
public interface RALCallable {
	/**
	 * Given some arguments, converts to an expression.
	 * The expression presumably does whatever it has to do to make this work.
	 */
	RALExpr instance(RALExpr[] args);
}
