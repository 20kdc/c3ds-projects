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
 * Just get this out of Lexer
 */
public class ByteHistory {
	private InputStream input;

	public int lineNumber = 1;

	// If not at byteHistory.length, the next byte to "read".
	private int byteHistoryPtr = 3;
	// this is in stream order, 
	private int[] byteHistory = new int[3];

	public ByteHistory(InputStream inp) {
		input = inp;
	}

	public int getNextByte() {
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

	public void backByte() {
		byteHistoryPtr--;
	}
}
