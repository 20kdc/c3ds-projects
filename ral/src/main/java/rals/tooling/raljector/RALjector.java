/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.awt.FileDialog;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;

import rals.Main;
import rals.code.ScriptSection;
import rals.debug.DummyDebugRecorder;
import rals.parser.IDocPath;

/**
 * GUI tool for injection and stuff.
 */
@SuppressWarnings("serial")
public class RALjector extends JFrame {
	// UI
	public final InjectStatusFrame injectFrame;
	public final JButton[] mainButtons;
	// Variables
	public final IDocPath stdLibDP;
	public File currentFile;
	public GameStateTracker debugState;
	public boolean injectWithDebugInfo;
	// UI Variables
	public final String[] mainButtonTexts = {
			"File: NONE",
			"Debug Info: OFF",
			"Collapse Buttons",
			"Inject Install/Events",
			"Install",
			"Events",
			"Remove"
	};
	public final String[] smallButtonTexts = {
			"FIL",
			"Db0",
			"Xpd",
			"I/E",
			"I",
			"E",
			"R",
			"D"
	};
	public boolean isSmall;

	public RALjector(final IDocPath std) {
		super("RALjector");
		stdLibDP = std;
		injectFrame = new InjectStatusFrame(this);
		mainButtons = new JButton[mainButtonTexts.length];
		final Runnable[] actions = new Runnable[] {
				() -> {
					FileDialog fd = new FileDialog(RALjector.this);
					fd.setFile("*.ral");
					fd.setVisible(true);
					File[] fs = fd.getFiles();
					if (fs.length == 1) {
						currentFile = fs[0];
						updateTexts();
					}
				},
				() -> {
					injectWithDebugInfo = !injectWithDebugInfo;
					updateTexts();
				},
				() -> {
					isSmall = !isSmall;
					updateTexts();
					pack();
				},
				() -> {
					injectUI(ScriptSection.Events, ScriptSection.Install);
				},
				() -> {
					injectUI(ScriptSection.Install);
				},
				() -> {
					injectUI(ScriptSection.Events);
				},
				() -> {
					injectUI(ScriptSection.Remove);
				}
		};
		setTitle("RALjector");
		setLayout(new GridLayout(0, 1));
		setAlwaysOnTop(true);
		for (int i = 0; i < mainButtons.length; i++) {
			final int iF = i;
			mainButtons[i] = new JButton("?");
			mainButtons[i].addActionListener((a) -> {
				actions[iF].run();
			});
			add(mainButtons[i]);
		}
		updateTexts();
		pack();
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	public void updateTexts() {
		if (currentFile != null) {
			mainButtonTexts[0] = "File: " + currentFile.getName();
		} else {
			mainButtonTexts[0] = "File: NONE";
		}
		mainButtonTexts[1] = "Debug Info: " + (injectWithDebugInfo ? "ON" : "OFF");
		smallButtonTexts[1] = "DI" + (injectWithDebugInfo ? "1" : "0");
		for (int i = 0; i < mainButtons.length; i++)
			mainButtons[i].setText(isSmall ? smallButtonTexts[i] : mainButtonTexts[i]);
	}

	private void injectUI(ScriptSection... sections) {
		StringBuilder sb = new StringBuilder();
		if (currentFile == null) {
			sb.append("No file!");
		} else {
			if (Main.inject(sb, stdLibDP, currentFile, new DummyDebugRecorder(), sections)) {
				sb.append("\nInject successful.");
			} else {
				sb.append("\nInject failed.");
			}
		}
		injectFrame.injectTextArea.setText(sb.toString());
		injectFrame.setVisible(true);
	}
}
