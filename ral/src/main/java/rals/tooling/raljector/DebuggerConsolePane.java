/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
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
	public LinkedList<String> history = new LinkedList<>();
	public int historyIndex = -1;
	public DebuggerConsolePane(Function<String, String> c) {
		communicator = c;
		textPane.setEditable(false);
		setLayout(new BorderLayout());
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);
		add(textField, BorderLayout.SOUTH);
		textField.addActionListener((a) -> {
			String req = textField.getText();
			// actual input, record to history if not equal to last
			recordToHistoryIfNotDuplicate(req);
			textField.setText("");
			fakeInput(req);
		});
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent var1) {
				if (var1.getKeyCode() == KeyEvent.VK_UP) {
					historyIndex--;
					if (historyIndex < 0) {
						historyIndex = 0;
					} else {
						historyIntoField();
					}
				} else if (var1.getKeyCode() == KeyEvent.VK_DOWN) {
					historyIndex++;
					if (historyIndex >= history.size()) {
						historyIndex = history.size() - 1;
					} else {
						historyIntoField();
					}
				}
			}
		});
		putText("RALjector Debug Console\n");
		putText("Type 'help' for help, 'cls' to clear buffer\n");
		putText("Text prefixed with '/' is sent as CAOS\n");
	}

	private void recordToHistoryIfNotDuplicate(String req) {
		if (!history.isEmpty())
			if (history.getLast().equals(req))
				return;
		history.add(req);
		historyIndex = history.size();
	}

	private void historyIntoField() {
		if (historyIndex < 0)
			return;
		if (historyIndex >= history.size())
			return;
		textField.setText(history.get(historyIndex));
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
		if (req.equalsIgnoreCase("cls")) {
			textPane.setText("");
			return;
		}
		try {
			putText(communicator.apply(req));
		} catch (Exception ex) {
			ex.printStackTrace();
			putText(ex.getMessage() + "\n");
		}
	}
}
