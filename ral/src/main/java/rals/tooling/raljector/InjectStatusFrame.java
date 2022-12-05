/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * Injection status.
 */
@SuppressWarnings("serial")
public class InjectStatusFrame extends JDialog {
	private final JTextPane injectTextArea = new JTextPane();
	public InjectStatusFrame(JFrame jf, GameStateTracker gst) {
		super(jf, "Inject");
		setSize(400, 300);
		injectTextArea.setEditable(false);
		setContentPane(new JScrollPane(injectTextArea));
		gst.displayMessageToUser.add((st) -> {
			injectTextArea.setText(st);
			injectTextArea.setCaretPosition(0);
			setVisible(true);
		});
	}
}
