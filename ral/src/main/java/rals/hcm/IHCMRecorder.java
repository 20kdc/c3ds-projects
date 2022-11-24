/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import rals.code.ScopeContext;
import rals.lex.Token;
import rals.stmt.*;

/**
 * Hover and Completion Model flight recorder interface.
 */
public interface IHCMRecorder {
	/**
	 * Run for every token read.
	 */
	void readToken(Token tkn);

	/**
	 * Marks a token as a reference to an ID.
	 */
	void idReference(Token tkn);

	/**
	 * Logs a given statement resolve.
	 * This is used to mine data about the variables and so forth in that statement.
	 * This is called from RALStatementUR.resolve (and nowhere else)
	 */
	void statementResolvePre(RALStatementUR rs, ScopeContext scope);

	/**
	 * Logs a given statement resolve.
	 * This is used to mine data about the variables and so forth in that statement.
	 * This is called from RALStatementUR.resolve (and nowhere else)
	 */
	void statementResolvePost(RALStatementUR rs, ScopeContext scope);
}
