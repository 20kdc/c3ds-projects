/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import rals.caos.CAOSUtils;
import rals.tooling.Injector;

/**
 * Dialog that monitors a value in the debugger.
 */
@SuppressWarnings("serial")
public class ValueMonitorDialog extends JFrame {
	public ValueMonitorDialog(GameStateTracker trk, String sourceName, Supplier<String> value) {
		// SOURCE... TEXT... REFRESH
		// (vars...)
		super(sourceName);
		setLayout(new BorderLayout());
		// top bar
		JPanel boxTop = new JPanel();
		boxTop.setLayout(new BorderLayout());
		JLabel sourceNameLabel = new JLabel(sourceName);
		boxTop.add(sourceNameLabel, BorderLayout.WEST);
		final JTextPane valuePane = new JTextPane();
		valuePane.setEditable(false);
		boxTop.add(valuePane, BorderLayout.CENTER);
		add(boxTop, BorderLayout.NORTH);
		// agent details panel
		JLabel agentHeader = new JLabel("");
		JPanel agentDetailsPanel = new JPanel();
		agentDetailsPanel.setLayout(new BorderLayout());
		agentDetailsPanel.add(agentHeader, BorderLayout.NORTH);
		// vars panel
		JPanel varsPanel = new JPanel();
		varsPanel.setLayout(new GridLayout(0, 4));
		final JButton[] buttons = new JButton[100];
		final String[] currentOV = new String[100];
		for (int i = 0; i < buttons.length; i++) {
			currentOV[i] = CAOSUtils.vaToString("ov", i);
			buttons[i] = new JButton("--------");
			final int myVarId = i;
			buttons[i].addActionListener((a) -> {
				String str = value.get();
				if (str != null) {
					try {
						final int validUnid = Integer.parseInt(str);
						// if we got this far, it's valid enough that we can make this happen
						new ValueMonitorDialog(trk, validUnid + "." + currentOV[myVarId], () -> {
							try {
								String res = Injector.cpxRequest("execute\ntarg agnt " + validUnid + " outx dbga " + myVarId);
								return CAOSUtils.unescapeOUTX(res.trim());
							} catch (Exception ex) {
								// do this because these errors can get weird
								ex.printStackTrace();
								return null;
							}
						});
					} catch (Exception ex) {
						// nope!
					}
				}
			});
			varsPanel.add(buttons[i]);
		}
		agentDetailsPanel.add(varsPanel, BorderLayout.CENTER);
		add(agentDetailsPanel, BorderLayout.CENTER);
		// Initial refresh
		Runnable refresh = () -> {
			String res = value.get();
			if (res != null) {
				sourceNameLabel.setText(sourceName);
				valuePane.setText(res);
				try {
					final int validUnid = Integer.parseInt(res);
					// if we got this far, it's valid enough that we can make this happen
					String code =
							"execute\n" +
							"targ agnt " + validUnid + "\n" +
							"setv va00 0\n" +
							"reps 100 outx dbga va00 outs \"\\n\" addv va00 1 repe\n";
					String allAVarRes = Injector.cpxRequest(code);
					String[] allAVarEsc = allAVarRes.split("\n");
					for (int i = 0; i < buttons.length; i++) {
						String unescaped = CAOSUtils.unescapeOUTX(allAVarEsc[i]);
						String name = CAOSUtils.vaToString("ov", i);
						currentOV[i] = name;
						buttons[i].setText(name + ": " + unescaped);
					}
				} catch (Exception ex) {
					// nope!
					agentHeader.setText("unable to get AVars: " + ex.getMessage());
				}
			} else {
				sourceNameLabel.setText(sourceName + " (INVALID)");
			}
		};
		refresh.run();
		// Done
		pack();
		final Consumer<Object> refreshEv = (x) -> refresh.run();
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
