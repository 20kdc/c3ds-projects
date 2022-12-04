/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * The magical debugging view
 */
@SuppressWarnings("serial")
public class CodeViewPane extends JScrollPane {
	private final JTextPane interiorTextPane;
	private final Style thisLine;
	public CodeViewPane() {
		interiorTextPane = new JTextPane();
		interiorTextPane.setEditable(false);
		setViewportView(interiorTextPane);
		thisLine = interiorTextPane.addStyle("thisLine", null);
		StyleConstants.setBackground(thisLine, Color.red);
		setMinimumSize(new Dimension(512, 512));
	}
	public void setCodeContents(Contents c) {
		String[] textLines = c.text.split("\n");
		try {
			StyledDocument sd = interiorTextPane.getStyledDocument();
			sd.remove(0, sd.getLength());
			int savedOffset = 0;
			for (int i = 0; i < textLines.length; i++) {
				if (c.line == i)
					savedOffset = sd.getLength();
				sd.insertString(sd.getLength(), textLines[i], c.line == i ? thisLine : null);
				sd.insertString(sd.getLength(), "\n", null);
			}
			interiorTextPane.setCaretPosition(savedOffset);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static class Contents {
		public final String text;
		public final int line;
		public Contents(String t, int l) {
			text = t;
			line = l;
		}
	}
}
