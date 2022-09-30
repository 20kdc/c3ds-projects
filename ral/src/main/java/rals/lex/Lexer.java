/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.io.IOException;
import java.io.InputStream;

import rals.lex.Token.Int;

/**
 * Big monolith.
 */
public class Lexer {
	private InputStream input;
	private boolean byteSaved;
	private int lastByte = -1;
	private boolean tokenSaved;
	private Token lastToken;
	private int lineNumber = 1;
	private static final String LONERS = ";[]{}(),";
	private static final String OPERATORS_BREAKING = "<>=?!/*-+:&|^%";
	private static final String OPERATORS_UNBREAKING = ".";
	private static final String OPERATORS = OPERATORS_BREAKING + OPERATORS_UNBREAKING;
	// note that the '#' breaker (comment) is inducted into whitespace
	private static final String BREAKERS = "\"\'#" + LONERS + OPERATORS_BREAKING;

	private String fileName;

	public Lexer(String fn, InputStream inp) {
		input = inp;
		fileName = fn;
	}

	private int getNextByte() {
		if (byteSaved) {
			byteSaved = false;
			return lastByte;
		}
		try {
			int val = input.read();
			if (val == 10)
				lineNumber++;
			lastByte = val;
			return val;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	private void backByte() {
		if (byteSaved)
			throw new RuntimeException("Can't go back two bytes in a row!");
		byteSaved = true;
	}

	private boolean consumeWS() {
		int b = getNextByte();
		boolean didAnything = false;
		while (true) {
			if (b == -1)
				break;
			if (b == '#') {
				while ((b != -1) && (b != 10))
					b = getNextByte();
				// ate the newline, that's alright
				didAnything = true;
				continue;
			}
			if (b > 32)
				break;
			b = getNextByte();
			didAnything = true;
		}
		backByte();
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
		} else if (LONERS.indexOf(c) != -1) {
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
		throw new RuntimeException("Expected " + kw);
	}

	public String requireNextID() {
		Token tkn = requireNext();
		if (tkn instanceof Token.ID)
			return ((Token.ID) tkn).text;
		throw new RuntimeException("Expected ID");
	}

	public int requireNextInteger() {
		Token tkn = requireNext();
		if (tkn instanceof Token.Int)
			return ((Token.Int) tkn).value;
		throw new RuntimeException("Expected integer");
	}
}
