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

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

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
		dbgCommandsMenu.add(new JButtonWR("debug log", () -> {
			new DebugLogDialog(ds).setVisible(true);
		}));
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
					new ValueMonitorDialog(debugState, getNameFromPDF(processedFrame), () -> {
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
