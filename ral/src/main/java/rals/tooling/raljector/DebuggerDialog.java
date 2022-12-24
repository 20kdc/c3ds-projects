/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Supplier;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;

import rals.caos.CAOSUtils;
import rals.tooling.Injector;

/**
 * RAL's debugger.
 */
@SuppressWarnings("serial")
public class DebuggerDialog extends JFrame {
	public final CodeViewPane text = new CodeViewPane();
	public final ValueView[] vaSet = new ValueView[100];
	public final ValueView[] intrinsicSet = new ValueView[9];
	public ProcessedDebugFrame[] processedFrames;
	public final DefaultListModel<ProcessedDebugFrame> frameListModel = new DefaultListModel<>();
	public final JList<ProcessedDebugFrame> frameList = new JList<>(frameListModel);
	public ProcessedDebugFrame processedFrame;
	public final Signal<ProcessedDebugFrame[]> newPDFSet = new Signal<>();
	public final Signal<ProcessedDebugFrame> newPDF = new Signal<>();
	public final GameStateTracker debugState;
	public DebuggerConsolePane debuggerConsole;
	public FilterLibMode filterLib = FilterLibMode.stdlib;

	public DebuggerDialog(final GameStateTracker ds) {
		debugState = ds;
		setTitle("RALjector: Debugger");
		setLayout(new BorderLayout());
		// for commands menu, etc.
		debuggerConsole = new DebuggerConsolePane(new DebuggerConsoleImpl(debugState, this));
		// commands menu
		JPanel dbgCommandsMenu = new JPanel();
		dbgCommandsMenu.setLayout(new BoxLayout(dbgCommandsMenu, BoxLayout.X_AXIS));
		dbgCommandsMenu.add(new Macro("step", "s"));
		dbgCommandsMenu.add(new Macro("over", "so"));
		dbgCommandsMenu.add(new Macro("stmt", "ns"));
		dbgCommandsMenu.add(new Macro("continue", "c"));
		final JLabel statusLine = new JLabel("Idle...");
		dbgCommandsMenu.add(statusLine);
		// VAs set
		JPanel vasSetPane = new JPanel();
		vasSetPane.setLayout(new GridLayout(0, 1));
		for (int i = 0; i < vaSet.length; i++) {
			vaSet[i] = new VAValueView(i);
			vasSetPane.add(vaSet[i]);
		}
		// Intrinsics set
		JPanel intrinsicsSetPane = new JPanel();
		intrinsicsSetPane.setLayout(new GridLayout(0, 2));
		for (int i = 0; i < RawDebugFrame.INTRINSIC_NAMES.length; i++) {
			intrinsicSet[i] = new IntrinsicValueView(i);
			intrinsicsSetPane.add(intrinsicSet[i]);
		}
		// finish up
		add(dbgCommandsMenu, BorderLayout.NORTH);
		JScrollPane vaScroll = new JScrollPane(vasSetPane);
		JSplitPane intrinsicVASplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, intrinsicsSetPane, vaScroll);
		JSplitPane framesConsoleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(frameList), debuggerConsole);
		JSplitPane jsp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, text, framesConsoleSplit);
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, intrinsicVASplit, jsp2);
		add(jsp, BorderLayout.CENTER);
		setSize(800, 600);
		debugState.debugFrame.add((f) -> {
			setVisible(true);
			processedFrames = ProcessedDebugFrame.process(f);
			newPDFSet.fire(processedFrames);
			statusLine.setText(" Tacked @ " + f.inScript.toScrpLine() + "." + f.caosOffset + " ");
		});
		newPDFSet.add((set) -> {
			frameListModel.clear();
			ProcessedDebugFrame fallback = null;
			ProcessedDebugFrame wanted = null;
			for (ProcessedDebugFrame pdf : set) {
				if (fallback == null)
					fallback = pdf;
				if (wanted == null) {
					switch (filterLib) {
					case stdlib:
						if (!pdf.shouldAvoid)
							wanted = pdf;
						break;
					case caos:
						if (pdf.name.equals("CAOS"))
							wanted = pdf;
						break;
					default:
					case none:
						wanted = pdf;
						break;
					}
				}
				frameListModel.addElement(pdf);
			}
			// and pick default to select
			processedFrame = wanted == null ? fallback : wanted;
			newPDF.fire(processedFrame);
		});
		frameList.addListSelectionListener((se) -> {
			ProcessedDebugFrame pf = frameList.getSelectedValue();
			if (pf != null) {
				processedFrame = pf;
				newPDF.fire(processedFrame);
			}
		});
		newPDF.add((file) -> {
			text.setCodeContents(file.contents);
		});
		debugState.stateChange.add((s) -> {
			if (s != GameStateTracker.State.Tacked)
				statusLine.setText(" " + s.toString() + " ");
		});
	}

	public void openValueInspector(final String sourceName, final Supplier<String> value) {
		// SOURCE... TEXT... REFRESH
		// (vars...)
		JFrame frame = new JFrame(sourceName);
		frame.setLayout(new BorderLayout());
		// top bar
		JPanel boxTop = new JPanel();
		boxTop.setLayout(new BorderLayout());
		JLabel sourceNameLabel = new JLabel(sourceName);
		boxTop.add(sourceNameLabel, BorderLayout.WEST);
		final JTextPane valuePane = new JTextPane();
		valuePane.setEditable(false);
		boxTop.add(valuePane, BorderLayout.CENTER);
		frame.add(boxTop, BorderLayout.NORTH);
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
						openValueInspector(validUnid + "." + currentOV[myVarId], () -> {
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
		frame.add(agentDetailsPanel, BorderLayout.CENTER);
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
		// Add refresh button last
		JButton jbtn = new JButton("Refresh");
		jbtn.addActionListener((a) -> {
			// Refresh
			refresh.run();
		});
		boxTop.add(jbtn, BorderLayout.EAST);
		// Done
		frame.pack();
		frame.setVisible(true);
	}

	public class Macro extends JButton {
		public final String command;
		public Macro(String text, String cmd) {
			super(text);
			command = cmd;
			addActionListener((a) -> {
				debuggerConsole.fakeInput(command);
			});
		}
	}

	public abstract class ValueView extends JButton {
		public ValueView() {
			super("----");
			Dimension correctSize = new Dimension(0, getPreferredSize().height);
			setMinimumSize(correctSize);
			setPreferredSize(correctSize);
			newPDF.add((pdf) -> {
				setText(getNameFromPDF(pdf) + ": " + getValueFromPDF(pdf));
			});
			addActionListener((a) -> {
				if (processedFrame != null)
					openValueInspector(getNameFromPDF(processedFrame), () -> {
						if (processedFrame != null) {
							return getValueFromPDF(processedFrame);
						} else {
							return null;
						}
					});
			});
		}
		public abstract String getNameFromPDF(ProcessedDebugFrame pdf);
		public abstract String getValueFromPDF(ProcessedDebugFrame pdf);
	}

	public class IntrinsicValueView extends ValueView {
		public final int index;
		public IntrinsicValueView(int i) {
			index = i;
		}
		public String getNameFromPDF(ProcessedDebugFrame pdf) {
			return RawDebugFrame.INTRINSIC_NAMES[index];
		}
		public String getValueFromPDF(ProcessedDebugFrame pdf) {
			return pdf.base.intrinsics[index];
		}
	}

	public class VAValueView extends ValueView {
		public final int index;
		public VAValueView(int i) {
			index = i;
		}
		public String getNameFromPDF(ProcessedDebugFrame pdf) {
			return pdf.vaNames[index];
		}
		public String getValueFromPDF(ProcessedDebugFrame pdf) {
			return pdf.base.va[index];
		}
	}

	public enum FilterLibMode {
		none, caos, stdlib;
	}
}
