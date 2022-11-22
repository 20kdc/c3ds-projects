/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import rals.diag.SrcPos;
import rals.diag.SrcPosFile;

/**
 * Just get this out of Lexer
 */
public class CharHistory {
	private final Reader input;

	private int lineNumber = 0;
	private int linePosition = 0;

	// If not at charHistory.length, the next byte to "read".
	private int charHistoryPtr;
	// This is in stream order.
	
	private final int[] charHistory;
	private final int[] lnHistory;
	private final int[] lpHistory;

	public CharHistory(Reader inp, int len) {
		charHistoryPtr = len;
		charHistory = new int[len];
		lnHistory = new int[len];
		lpHistory = new int[len];
		input = inp;
	}

	private void advanceHistory(int[] history, int val) {
		for (int i = 0; i < history.length - 1; i++)
			history[i] = history[i + 1];
		history[history.length - 1] = val;
	}

	public int getNextChar() {
		if (charHistoryPtr < charHistory.length)
			return charHistory[charHistoryPtr++];
		try {
			int val = input.read();
			// history updates
			advanceHistory(charHistory, val);
			advanceHistory(lnHistory, lineNumber);
			advanceHistory(lpHistory, linePosition);
			// line number/position updates
			if (val == 10) {
				lineNumber++;
				linePosition = 0;
			} else if (val != -1) {
				linePosition++;
			}
			// done!
			return val;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public void backChar() {
		charHistoryPtr--;
	}

	/**
	 * Returns the position of the next character.
	 */
	public SrcPos genLN(SrcPosFile file) {
		if (charHistoryPtr < charHistory.length)
			return new SrcPos(file, lnHistory[charHistoryPtr], lpHistory[charHistoryPtr]);
		return new SrcPos(file, lineNumber, linePosition);
	}
}
