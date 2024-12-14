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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import cdsp.common.cpx.Injector;
import rals.caos.CAOSUtils;
import rals.types.Classifier;

/**
 * Dialog that monitors a value in the debugger.
 */
@SuppressWarnings("serial")
public class ValueMonitorDialog extends JFrame {
	private final JLabel sourceNameLabel = new JLabel();
	private final JTextPane valuePane = new JTextPane();
	private final JLabel agentHeader = new JLabel("");
	private String currentSourceName;
	private Supplier<String> currentValue;
	private final JButton[] buttons = new JButton[100];
	private final String[] currentOV = new String[100];
	private final GameStateTracker state;

	public ValueMonitorDialog(GameStateTracker trk, String initSourceName, Supplier<String> initValue) {
		state = trk;
		// SOURCE... TEXT... BUTTONS...
		// (vars...)
		setLayout(new BorderLayout());
		// top bar, right side
		JPanel boxTopRightButtons = new JPanel();
		boxTopRightButtons.setLayout(new BoxLayout(boxTopRightButtons, BoxLayout.X_AXIS));
		JButton buttonToTackAgent = new JButton("TACK");
		buttonToTackAgent.addActionListener((a) -> {
			String str = currentValue.get();
			if (str != null) {
				try {
					final int validUnid = Integer.parseInt(str);
					Injector.cpxRequest("execute\ndbg: tack agnt " + validUnid, CAOSUtils.CAOS_CHARSET);
				} catch (Exception ex) {
					ex.printStackTrace();
					trk.displayMessageToUser.fire("Unable to TACK: " + ex.toString());
				}
			}
		});
		boxTopRightButtons.add(buttonToTackAgent);
		JButton buttonToLockAgent = new JButton("Lock");
		buttonToLockAgent.addActionListener((a) -> {
			final String res = initValue.get();
			if (res != null)
				setSource("lock:" + res, () -> res);
		});
		boxTopRightButtons.add(buttonToLockAgent);
		// top bar
		JPanel boxTop = new JPanel();
		boxTop.setLayout(new BorderLayout());
		boxTop.add(sourceNameLabel, BorderLayout.WEST);
		valuePane.setEditable(false);
		boxTop.add(valuePane, BorderLayout.CENTER);
		boxTop.add(boxTopRightButtons, BorderLayout.EAST);
		add(boxTop, BorderLayout.NORTH);
		// agent details panel
		JPanel agentDetailsPanel = new JPanel();
		agentDetailsPanel.setLayout(new BorderLayout());
		agentDetailsPanel.add(agentHeader, BorderLayout.NORTH);
		// vars panel
		JPanel varsPanel = new JPanel();
		varsPanel.setLayout(new GridLayout(0, 4));
		for (int i = 0; i < buttons.length; i++) {
			currentOV[i] = CAOSUtils.vaToString("ov", i);
			buttons[i] = new JButton("--------");
			final int myVarId = i;
			buttons[i].addActionListener((a) -> {
				String str = currentValue.get();
				if (str != null) {
					try {
						final int validUnid = Integer.parseInt(str);
						// if we got this far, it's valid enough that we can make this happen
						new ValueMonitorDialog(trk, validUnid + "." + currentOV[myVarId], () -> {
							try {
								String res = Injector.cpxRequest("execute\ntarg agnt " + validUnid + " outx dbga " + myVarId, CAOSUtils.CAOS_CHARSET);
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
		setSource(initSourceName, initValue);
		// Done
		pack();
		final Consumer<Object> refreshEv = (x) -> refreshValue();
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
	public void setSource(String sourceName, Supplier<String> value) {
		currentSourceName = sourceName;
		currentValue = value;
		setTitle(sourceName);
		refreshValue();
	}
	public void refreshValue() {
		String res = currentValue.get();
		if (res != null) {
			sourceNameLabel.setText(currentSourceName);
			valuePane.setText(res);
			try {
				final int validUnid = Integer.parseInt(res);
				// if we got this far, it's valid enough that we can make this happen
				String code =
						"execute\n" +
						"targ agnt " + validUnid + "\n" +
						"doif targ <> null\n" +
						" outv type targ\n" +
						" outs \" \"\n" +
						" outv fmly\n" +
						" outs \" \"\n" +
						" outv gnus\n" +
						" outs \" \"\n" +
						" outv spcs\n" +
						" outs \"\\n\"\n" +
						" setv va00 0\n" +
						" reps 100 outx dbga va00 outs \"\\n\" addv va00 1 repe\n" +
						"else\n" +
						" outs \"null\\n\"\n" +
						"endi\n";
				String allAVarRes = Injector.cpxRequest(code, CAOSUtils.CAOS_CHARSET);
				String[] allAVarEsc = allAVarRes.split("\n");
				if (allAVarEsc[0].equals("null")) {
					agentHeader.setText("not a valid agent");
				} else {
					String[] classifier = allAVarEsc[0].split(" ");
					int aType = Integer.parseInt(classifier[0]);
					int aFmly = Integer.parseInt(classifier[1]);
					int aGnus = Integer.parseInt(classifier[2]);
					int aSpcs = Integer.parseInt(classifier[3]);
					Classifier aClassifier = new Classifier(aFmly, aGnus, aSpcs);
					// figure out native class
					String nativeClassName = "Agent("+ aType+ ")";
					if (aType == 3)
						nativeClassName = "Simple";
					else if (aType == 4)
						nativeClassName = "Pointer";
					else if (aType == 5)
						nativeClassName = "Compound";
					else if (aType == 6)
						nativeClassName = "Vehicle";
					else if (aType == 7)
						nativeClassName = "Creature";
					// figure out script class
					String scriptClassName = "Agent";
					DebugTaxonomyData.Entry ent = null;
					if (state.debugTaxonomy != null)
						ent = state.debugTaxonomy.getBestEntry(aClassifier);
					if (ent != null)
						scriptClassName = ent.name;
					agentHeader.setText(nativeClassName + " " + scriptClassName + aClassifier);
					for (int i = 0; i < buttons.length; i++) {
						String unescaped = CAOSUtils.unescapeOUTX(allAVarEsc[i + 1]);
						if (ent != null) {
							currentOV[i] = ent.objectVariables[i];
						} else {
							currentOV[i] = CAOSUtils.vaToString("ov", i);
						}
						buttons[i].setText(currentOV[i] + ": " + unescaped);
					}
				}
			} catch (Exception ex) {
				// nope!
				agentHeader.setText("unable to get AVars: " + ex.getMessage());
			}
		} else {
			sourceNameLabel.setText(currentSourceName + " (INVALID)");
		}
	}
}
