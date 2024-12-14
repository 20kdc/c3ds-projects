/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;

import cdsp.common.cpx.Injector;
import rals.caos.CAOSUtils;

/**
 * For viewing the debug log.
 */
@SuppressWarnings("serial")
public class DebugLogDialog extends JFrame {
	private final JTextPane valuePane = new JTextPane();

	public DebugLogDialog(GameStateTracker trk) {
		super("Debug Log");
		setContentPane(new JScrollPane(valuePane));
		setSize(400, 300);
		final Consumer<Object> refreshEv = (x) -> {
			try {
				String res = Injector.cpxRequest("execute\ndbg: poll\n", CAOSUtils.CAOS_CHARSET);
				Document doc = valuePane.getDocument();
				doc.insertString(doc.getLength(), res, null);
			} catch (Exception ex) {
				// do not care
			}
		};
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				trk.additionalRefreshTasks.add(refreshEv);
			}
			@Override
			public void windowClosed(WindowEvent e) {
				trk.additionalRefreshTasks.remove(refreshEv);
			}
		});
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);
	}
}
