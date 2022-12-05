/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.BorderLayout;
import java.util.function.Function;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

/**
 * Oh dear goodness.
 */
@SuppressWarnings("serial")
public class DebuggerConsolePane extends JPanel {
	public final JTextField textField = new JTextField();
	public final JTextPane textPane = new JTextPane();
	public final JScrollPane scrollPane = new JScrollPane(textPane);
	public final Function<String, String> communicator;
	public DebuggerConsolePane(Function<String, String> c) {
		communicator = c;
		textPane.setEditable(false);
		setLayout(new BorderLayout());
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);
		add(textField, BorderLayout.SOUTH);
		textField.addActionListener((a) -> {
			String req = textField.getText();
			textField.setText("");
			fakeInput(req);
		});
		putText("RALjector Debug Console\n");
		putText("Type 'help' for help\n");
	}

	public void putText(String text) {
		StyledDocument sd = textPane.getStyledDocument();
		try {
			sd.insertString(sd.getLength(), text, null);
		} catch (Exception ex) {
			// NO.
		}
		textPane.setCaretPosition(sd.getLength());
	}

	public void fakeInput(String req) {
		putText("> " + req + "\n");
		try {
			putText(communicator.apply(req));
		} catch (Exception ex) {
			ex.printStackTrace();
			putText("Exception: " + ex.getMessage() + "\n");
		}
	}
}
