/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.io.Reader;

import rals.diag.DiagRecorder;
import rals.diag.SrcPos;
import rals.diag.SrcPosFile;
import rals.hcm.IHCMRecorder;

/**
 * Big monolith.
 */
public class Lexer {
	public final int TOKEN_HISTORY_LEN = 5;

	/**
	 * This points to the next token to return.
	 * So the last token we returned is -1 from that.
	 */
	private int tokenHistoryPtr = TOKEN_HISTORY_LEN;
	private Token[] tokenHistory = new Token[TOKEN_HISTORY_LEN];

	public final DiagRecorder diags;
	public final IHCMRecorder hcm;
	private final LexerCore core;

	private boolean tracing = false;

	public Lexer(SrcPosFile fn, Reader inp, DiagRecorder d, IHCMRecorder h) {
		diags = d;
		hcm = h;
		core = new LexerCore(fn, inp, d);
	}

	public SrcPos genLN() {
		Token nxt = next();
		back();
		if (nxt == null)
			return core.genLN();
		return nxt.lineNumber;
	}

	private Token getLastToken() {
		return tokenHistory[tokenHistoryPtr - 1];
	}

	/**
	 * Generates definition info from the range including the given start token and the last token retrieved.
	 */
	public DefInfo.At genDefInfo(Token tkn) {
		return new DefInfo.At(tkn, getLastToken());
	}

	public Token next() {
		if (tokenHistoryPtr < tokenHistory.length) {
			Token res = tokenHistory[tokenHistoryPtr++];
			traceEvent(res);
			if (res != null)
				hcm.parserRequestedToken(res, true);
			return res;
		}
		Token tkn = null;
		while (true) {
			tkn = core.get();
			if (tkn instanceof Token.Kw) {
				// Lexer debug
				// note that we don't "count" this token for even HCM!!!
				if (((Token.Kw) tkn).text.equals(Token.DEBUG_TRACE_ON)) {
					tracing = true;
					continue;
				} else if (((Token.Kw) tkn).text.equals(Token.DEBUG_TRACE_OFF)) {
					tracing = false;
					continue;
				}
			}
			break;
		}
		traceEvent(tkn);
		if (tkn != null) {
			hcm.readToken(tkn);
			hcm.parserRequestedToken(tkn, true);
		}
		for (int i = 0; i < tokenHistory.length - 1; i++)
			tokenHistory[i] = tokenHistory[i + 1];
		tokenHistory[tokenHistory.length - 1] = tkn;
		return tkn;
	}

	public void back() {
		traceEvent(tokenHistory[tokenHistoryPtr - 1]);
		if (tokenHistory[tokenHistoryPtr - 1] == null)
			throw new RuntimeException("Attempted to go back before the start of the file / locked backwards travel cutoff!");
		tokenHistoryPtr--;
		// Keep HCM in sync by updating last requested token to the one before the one we're going to return on next().
		// Completion intents that target the token we'll return on next() therefore will anchor on the token before it.
		// (This is expected and is how completion intents work.)
		hcm.parserRequestedToken(tokenHistory[tokenHistoryPtr - 1], false);
	}

	private void traceEvent(Token token) {
		if (tracing) {
			String msg = "LX:" + (token != null ? token : "EOF");
			StackTraceElement[] ste = new RuntimeException().getStackTrace();
			for (int i = 1; i < 5; i++) {
				if (i >= ste.length)
					break;
				msg += " " + ste[i].getMethodName() + ":" + ste[i].getLineNumber();
			}
			diags.warning(msg);
		}
	}

	public Token requireNext() {
		Token tkn = next();
		if (tkn == null)
			throw new RuntimeException("Expected token, got EOF at " + genLN());
		return tkn;
	}

	public Token requireNextKw(String kw) {
		Token tkn = requireNext();
		if (tkn.isKeyword(kw))
			return tkn;
		throw new RuntimeException("Expected " + kw + ", got " + tkn);
	}

	public boolean optNextKw(String string) {
		Token tkn = next();
		if (tkn == null)
			return false;
		if (tkn.isKeyword(string))
			return true;
		back();
		return false;
	}

	public String requireNextID() {
		return requireNextIDTkn().text;
	}

	public Token.ID requireNextIDTkn() {
		Token tkn = requireNext();
		if (tkn instanceof Token.ID)
			return (Token.ID) tkn;
		throw new RuntimeException("Expected ID, got " + tkn);
	}

	public int requireNextInteger() {
		Token tkn = requireNext();
		if (tkn instanceof Token.Int)
			return ((Token.Int) tkn).value;
		throw new RuntimeException("Expected integer, got " + tkn);
	}

	/**
	 * Returns context as a string for diagnostics
	 */
	public String diagContext() {
		StringBuilder sb = new StringBuilder();
		boolean didWriteAtLeastOne = false;
		for (int i = 0; i < tokenHistoryPtr; i++) {
			if (tokenHistory[i] == null)
				continue;
			if (didWriteAtLeastOne)
				sb.append(" ");
			sb.append(tokenHistory[i]);
			didWriteAtLeastOne = true;
		}
		if (didWriteAtLeastOne)
			sb.append(" ");
		sb.append("<HERE>");
		Token tkn = next();
		back();
		if (tkn != null) {
			sb.append(" ");
			sb.append(tkn);
		} else {
			sb.append(" <EOF>");
		}
		return sb.toString();
	}
}
