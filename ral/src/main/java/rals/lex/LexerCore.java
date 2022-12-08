/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.io.Reader;
import java.util.function.Supplier;

import rals.diag.DiagRecorder;
import rals.diag.SrcPos;
import rals.diag.SrcPosFile;
import rals.diag.SrcRange;

/**
 * Internal lexer stuff. Refactored out of Lexer because the interface stuff was getting very complex.
 * This specifically should NEVER gain history support or anything like that.
 */
public class LexerCore implements Supplier<Token> {
	private final SrcPosFile file;
	private final CharHistory charHistory;
	private final DiagRecorder diags;

	private static final String LONERS = ";[]{}(),.";
	private static final String NUM_START = "+-0123456789";
	private static final String NUM_BODY = "0123456789.e";
	private static final String OPERATORS_BREAKING = "<>=?!/*-+:&|^%~@";
	private static final String OPERATORS_UNBREAKING = "";
	private static final String OPERATORS = OPERATORS_BREAKING + OPERATORS_UNBREAKING;
	private static final String BREAKERS = "\"\'" + LONERS + OPERATORS_BREAKING;

	private int levelOfStringEmbedding = 0;
	private int levelOfStringEmbeddingEscape = 0;

	/**
	 * Last comment encountered.
	 * Can be reset to null to consume a comment.
	 */
	private String lastComment = null;

	public LexerCore(SrcPosFile fn, Reader inp, DiagRecorder d) {
		charHistory = new CharHistory(inp, 3);
		file = fn;
		diags = d;
	}

	/**
	 * Consume a comment.
	 * This is used to assemble documentation comments.
	 */
	private String consumeComment() {
		String s = lastComment;
		lastComment = null;
		return s;
	}

	public SrcPos genLN() {
		return charHistory.genLN(file);
	}


	private SrcRange completeExtent(SrcPos sp) {
		return new SrcRange(sp, charHistory.genLN(file));
	}

	/**
	 * This now returns a char rather than a byte.
	 * Since the conversion was done by cast anyway, this isn't a problem...
	 */
	private int getNextByte() {
		return charHistory.getNextChar();
	}

	private void backByte() {
		charHistory.backChar();
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
					// This is only used for diagnostics, so it doesn't need to be character-precise.
					SrcPos at = genLN();
					StringBuilder sb = new StringBuilder();
					// Block comment.
					while (true) {
						b = getNextByte();
						if (b == -1) {
							diags.lexParseErr(at, "Unterminated block comment");
							break;
						}
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
					lastComment = sb.toString().trim();
					if (lastComment.length() == 0)
						lastComment = null;
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
					lastComment = sb.toString().trim();
					if (lastComment.length() == 0)
						lastComment = null;
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

	/**
	 * Retrieves the next token from the file.
	 */
	@Override
	public Token get() {
		consumeWS();
		// Get this after consuming whitespace but before grabbing what will be the start character of the token.
		SrcPos startOfToken = genLN();
		int c = getNextByte();
		if (c == -1)
			return null;
		if (c == '\"') {
			// Regular ol' string
			return finishReadingString(startOfToken, c, false, false);
		} else if (c == '\'') {
			// String w/ string embedding capabilities
			return finishReadingString(startOfToken, c, false, true);
		} else if ((c == '}') && (levelOfStringEmbedding > 0) && (levelOfStringEmbeddingEscape == 0)) {
			// Leaving string embedding argument and entering the string part again
			levelOfStringEmbedding--;
			return finishReadingString(startOfToken, '\'', true, true);
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
					return new Token.Int(completeExtent(startOfToken), consumeComment(), Integer.parseInt(str));
				} catch (Exception ex) {
					// nope
				}
				try {
					return new Token.Flo(completeExtent(startOfToken), consumeComment(), Float.parseFloat(str));
				} catch (Exception ex) {
					// nope
				}
				SrcPos sp = genLN();
				diags.lexParseErr(sp, "number-like not number");
				return new Token.ID(new SrcRange(startOfToken, sp), consumeComment(), str);
			}
		}
		if (LONERS.indexOf(c) != -1) {
			if (levelOfStringEmbedding > 0) {
				if (c == '{')
					levelOfStringEmbeddingEscape++;
				if (c == '}')
					if (levelOfStringEmbeddingEscape > 0)
						levelOfStringEmbeddingEscape--;
			}
			return new Token.Kw(completeExtent(startOfToken), consumeComment(), Character.toString((char) c));
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
			return new Token.Kw(completeExtent(startOfToken), consumeComment(), str);
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
				return new Token.Kw(completeExtent(startOfToken), consumeComment(), str);
			} else {
				return new Token.ID(completeExtent(startOfToken), consumeComment(), str);
			}
		}
	}

	private Token finishReadingString(SrcPos startOfToken, int c, boolean startIsClusterEnd, boolean isEmbedding) {
		StringBuilder sb = new StringBuilder();
		boolean escaping = false;
		boolean endIsClusterStart = false;
		while (true) {
			int c2 = getNextByte();
			if (c2 == -1) {
				diags.lexParseErr(startOfToken, "Unterminated string");
				break;
			}
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
				} else if (isEmbedding && (c2 == '{')) {
					levelOfStringEmbedding++;
					endIsClusterStart = true;
					break;
				} else if (c2 == '\\') {
					escaping = true;
				} else {
					sb.append((char) c2);
				}
			}
		}
		SrcRange sp = completeExtent(startOfToken);
		if (isEmbedding) {
			return new Token.StrEmb(sp, consumeComment(), sb.toString(), startIsClusterEnd, endIsClusterStart);
		} else {
			return new Token.Str(sp, consumeComment(), sb.toString());
		}
	}
}
