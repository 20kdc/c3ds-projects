/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.nio.charset.Charset;

/**
 * It's that thing that does the stuff!
 */
public class CodeWriter {
	/**
	 * Character set for a standard copy of Creatures 3 or Docking Station.
	 */
	public static final Charset CAOS_CHARSET = Charset.forName("Cp1252");

	private StringBuilder writer;
	public int indent;
	public String queuedCommentForNextLine = null;
	public final DebugType debugType;

	public CodeWriter(StringBuilder outText, DebugType dbg) {
		writer = outText;
		debugType = dbg;
	}

	private void writeIndent() {
		for (int i = 0; i < indent; i++)
			writer.append('\t');
	}

	private void writeNLC() {
		if (queuedCommentForNextLine != null) {
			String qc = queuedCommentForNextLine;
			queuedCommentForNextLine = null;
			writeComment(qc);
		}
	}

	public void writeComment(String comment) {
		writeNLC();
		writeIndent();
		writer.append(" * ");
		for (char c : comment.toCharArray()) {
			writer.append(c);
			if (c == '\n') {
				writeIndent();
				writer.append(" * ");
			}
		}
		writer.append('\n');
	}

	public void writeCode(int pre, String text, int post) {
		indent += pre;
		writeCode(text);
		indent += post;
	}

	public void writeCode(int pre, String text) {
		writeCode(pre, text, 0);
	}

	public void writeCode(String text, int post) {
		writeCode(0, text, post);
	}

	public void writeCode(String text) {
		writeNLC();
		writeIndent();
		for (char c : text.toCharArray()) {
			writer.append(c);
			if (c == '\n')
				writeIndent();
		}
		writer.append("\n");
	}
}
