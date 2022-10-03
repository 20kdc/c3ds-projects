/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.io.IOException;
import java.io.InputStream;

/**
 * Big monolith.
 */
public class Lexer {
	private InputStream input;

	// If not at byteHistory.length, the next byte to "read".
	private int byteHistoryPtr = 3;
	// this is in stream order, 
	private int[] byteHistory = new int[3];

	private boolean tokenSaved;
	private Token lastToken;
	private int lineNumber = 1;
	private static final String LONERS = ";[]{}(),.";
	private static final String NUM_START = "+-0123456789";
	private static final String NUM_BODY = "0123456789.e";
	private static final String OPERATORS_BREAKING = "<>=?!/*-+:&|^%";
	private static final String OPERATORS_UNBREAKING = "";
	private static final String OPERATORS = OPERATORS_BREAKING + OPERATORS_UNBREAKING;
	private static final String BREAKERS = "\"\'" + LONERS + OPERATORS_BREAKING;

	private String fileName;
	/**
	 * Last comment encountered.
	 * Can be reset to null by caller to consume a comment.
	 */
	public String lastComment = null;

	public Lexer(String fn, InputStream inp) {
		input = inp;
		fileName = fn;
	}

	private int getNextByte() {
		if (byteHistoryPtr < byteHistory.length)
			return byteHistory[byteHistoryPtr++];
		try {
			int val = input.read();
			if (val == 10)
				lineNumber++;
			// 
			for (int i = 0; i < byteHistory.length - 1; i++)
				byteHistory[i] = byteHistory[i + 1];
			byteHistory[byteHistory.length - 1] = val;
			return val;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	private void backByte() {
		byteHistoryPtr--;
	}

	private boolean consumeWS() {
		boolean didAnything = false;
		while (true) {
			int b = getNextByte();
			if (b == -1)
				break;
			if (b == '/') {
				// This *could* be a line comment, or it *could* just be something else.
				// Let's find out the difference...
				b = getNextByte();
				if (b == '*') {
					SrcPos at = genLN();
					StringBuilder sb = new StringBuilder();
					// Block comment.
					while (true) {
						b = getNextByte();
						if (b == -1)
							throw new RuntimeException("Unterminated block comment, starting at " + at);
						if (b == '*') {
							b = getNextByte();
							if (b == '/') {
								// End of block comment!
								break;
							} else {
								// just in case it was -1, let next loop grab it
								backByte();
							}
						}
						// not terminating the comment, so
						if ((b != 13))
							sb.append((char) b);
					}
					lastComment = sb.toString();
					didAnything = true;
					continue;
				} else if (b == '/') {
					// Line comment.
					StringBuilder sb = new StringBuilder();
					b = getNextByte();
					while ((b != -1) && (b != 10)) {
						if (b != 13)
							sb.append((char) b);
						b = getNextByte();
					}
					lastComment = sb.toString();
					// ate the newline, that's alright
					didAnything = true;
					continue;
				} else {
					// Whoopsie!
					backByte();
					backByte();
					break;
				}
			}
			if (b > 32) {
				backByte();
				break;
			}
			didAnything = true;
		}
		return didAnything;
	}

	public SrcPos genLN() {
		return new SrcPos(fileName, lineNumber);
	}

	public Token next() {
		if (tokenSaved) {
			tokenSaved = false;
			return lastToken;
		}
		consumeWS();
		int c = getNextByte();
		if (c == -1)
			return null;
		if ((c == '\"') || (c == '\'')) {
			StringBuilder sb = new StringBuilder();
			boolean escaping = false;
			while (true) {
				int c2 = getNextByte();
				if (c2 == -1)
					throw new RuntimeException("Unterminated string");
				if (escaping) {
					if (c2 == 'r')
						c2 = '\r';
					if (c2 == 't')
						c2 = '\t';
					if (c2 == 'n')
						c2 = '\n';
					if (c2 == '0')
						c2 = 0;
					sb.append((char) c2);
					escaping = false;
				} else {
					if (c2 == c) {
						break;
					} else if (c2 == '\\') {
						escaping = true;
					} else {
						sb.append((char) c2);
					}
				}
			}
			lastToken = new Token.Str(genLN(), sb.toString());
			return lastToken;
		}
		// This has to punch a gap in the nice little else-if chain.
		// Why? Because it needs to fallthrough if stuff goes wrong...
		if (NUM_START.indexOf(c) != -1) {
			boolean confirmed = false;
			StringBuilder sb = new StringBuilder();
			sb.append((char) c);
			if (NUM_BODY.indexOf(c) == -1) {
				// this isn't a NUM_BODY so check next char
				int c2 = getNextByte();
				sb.append((char) c2);
				if ((c2 == -1) || (NUM_BODY.indexOf(c2) == -1)) {
					// nope
					// Note that because we fallthrough to other token types, 'c' is still in play
					// Hence only go back one byte
					backByte();
				} else {
					confirmed = true;
				}
			} else {
				confirmed = true;
			}
			if (confirmed) {
				// alright, we have a number
				while (true) {
					c = getNextByte();
					if ((c == -1) || (NUM_BODY.indexOf(c) == -1)) {
						backByte();
						break;
					}
					sb.append((char) c);
				}
				String str = sb.toString();
				try {
					lastToken = new Token.Int(genLN(), Integer.parseInt(str));
					return lastToken;
				} catch (Exception ex) {
					// nope
				}
				try {
					lastToken = new Token.Flo(genLN(), Float.parseFloat(str));
					return lastToken;
				} catch (Exception ex) {
					// nope
				}
				throw new RuntimeException("number-like not number at " + genLN());
			}
		}
		if (LONERS.indexOf(c) != -1) {
			lastToken = new Token.Kw(genLN(), Character.toString((char) c));
			return lastToken;
		} else if (OPERATORS.indexOf(c) != -1) {
			StringBuilder sb = new StringBuilder();
			sb.append((char) c);
			while (true) {
				c = getNextByte();
				if ((c == -1) || (OPERATORS.indexOf(c) == -1)) {
					backByte();
					break;
				}
				sb.append((char) c);
			}
			String str = sb.toString();
			lastToken = new Token.Kw(genLN(), str);
			return lastToken;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append((char) c);
			while (true) {
				c = getNextByte();
				if ((c <= 32) || (BREAKERS.indexOf(c) != -1)) {
					backByte();
					break;
				}
				sb.append((char) c);
			}
			String str = sb.toString();
			if (Token.keywords.contains(str)) {
				lastToken = new Token.Kw(genLN(), str);
				return lastToken;
			} else {
				lastToken = new Token.ID(genLN(), str);
				return lastToken;
			}
		}
	}

	public void back() {
		if (tokenSaved)
			throw new RuntimeException("Can't go back more than a single token");
		tokenSaved = true;
	}

	public Token requireNext() {
		Token tkn = next();
		if (tkn == null)
			throw new RuntimeException("Expected token, got EOF");
		return tkn;
	}

	public void requireNextKw(String kw) {
		Token tkn = requireNext();
		if (tkn.isKeyword(kw))
			return;
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
		Token tkn = requireNext();
		if (tkn instanceof Token.ID)
			return ((Token.ID) tkn).text;
		throw new RuntimeException("Expected ID, got " + tkn);
	}

	public int requireNextInteger() {
		Token tkn = requireNext();
		if (tkn instanceof Token.Int)
			return ((Token.Int) tkn).value;
		throw new RuntimeException("Expected integer, got " + tkn);
	}
}
