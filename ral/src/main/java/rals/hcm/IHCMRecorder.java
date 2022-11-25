/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import rals.code.ScopeContext;
import rals.diag.SrcRange;
import rals.expr.RALExprSlice;
import rals.expr.RALExprUR;
import rals.lex.Token;

/**
 * Hover and Completion Model flight recorder interface.
 */
public interface IHCMRecorder {
	/**
	 * Run for every (non-EOF) token read.
	 */
	void readToken(Token tkn);

	/**
	 * Run whenever the token requested by the parser changes.
	 * actualRequest indicates the parser actually directly requested this token.
	 * That is as opposed to, say, back().
	 */
	void parserRequestedToken(Token tkn, boolean actualRequest);

	/**
	 * Adds a completion intent to the next requested token.
	 * If autoHover is true, will also set a hover intent automatically.
	 * This version will setup the stuff for contextual intents if params is not null.
	 */
	void addCompletionRelIntentToNextToken(HCMIntent intent, boolean autoHover, RALExprUR... params);

	/**
	 * Adds a completion intent to the next requested token, with possible 
	 * If autoHover is true, will also set a hover intent automatically.
	 */
	default void addCompletionIntentToNextToken(HCMIntent intent, boolean autoHover) {
		addCompletionRelIntentToNextToken(intent, autoHover, (RALExprUR[]) null);
	}

	/**
	 * Sets a token's hover intent.
	 */
	void setTokenHoverRelIntent(Token.ID tkn, HCMIntent intent, RALExprUR... params);

	/**
	 * Sets a token's hover intent.
	 */
	default void setTokenHoverIntent(Token.ID tkn, HCMIntent intent) {
		setTokenHoverRelIntent(tkn, intent, (RALExprUR[]) null);
	}

	/**
	 * Logs a given resolve.
	 * This is called from:
	 * + RALStatementUR.resolve
	 * + Macro.precompile
	 */
	void resolvePre(SrcRange rs, ScopeContext scope);

	/**
	 * See resolvePre.
	 */
	void resolvePost(SrcRange rs, ScopeContext scope);

	/**
	 * Logs an expression resolve.
	 * This is called from RALExprUR.resolve.
	 */
	void onResolveExpression(RALExprUR src, RALExprSlice dst);
}
