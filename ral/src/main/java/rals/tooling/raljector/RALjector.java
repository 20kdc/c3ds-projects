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
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.Timer;

import rals.Main;
import rals.code.CodeGenFeatureLevel;
import rals.code.OuterCompileContext;
import rals.code.ScriptSection;
import rals.code.Scripts;
import rals.debug.DummyDebugRecorder;
import rals.debug.FullDebugRecorder;
import rals.debug.IDebugRecorder;
import rals.parser.IDocPath;
import rals.parser.IncludeParseContext;
import rals.parser.Parser;

/**
 * GUI tool for injection and stuff.
 */
@SuppressWarnings("serial")
public class RALjector extends JFrame {
	// UI
	@SuppressWarnings("unused")
	private final InjectStatusFrame injectFrame;
	public final DebuggerDialog debugFrame;
	public final JButton[] mainButtons;
	// Variables
	public final IDocPath stdLibDP;
	public File currentFile;
	public final GameStateTracker debugState = new GameStateTracker();
	public boolean injectWithDebugInfo;
	// UI Variables
	public final String[] mainButtonTexts = {
			"File: NONE",
			"Debug Info: OFF",
			"Open Debugger",
			"Collapse Buttons",
			"Inject Install/Events",
			"Install",
			"Events",
			"Remove",
			"View CAOS"
	};
	public final String[] smallButtonTexts = {
			"FIL",
			"Db0",
			"Dbg",
			"Xpd",
			"I/E",
			"I",
			"E",
			"R",
			"D",
			"VC"
	};
	public boolean isSmall;

	public RALjector(final IDocPath std) {
		super("RALjector");
		stdLibDP = std;
		injectFrame = new InjectStatusFrame(this, debugState);
		debugFrame = new DebuggerDialog(debugState);
		new Timer(50, (a) -> {
			debugState.update();
		}).start();
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
					debugFrame.setVisible(true);
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
				},
				() -> {
					if (currentFile == null) {
						debugState.displayMessageToUser.fire("No file!");
						return;
					}
					StringBuilder sb = new StringBuilder();
					try {
						IncludeParseContext ipc = Parser.run(std, currentFile);
						debugState.debugTaxonomy = new DebugTaxonomyData(ipc.typeSystem);
						Scripts scr = ipc.module.resolve(ipc.diags, ipc.hcm);
						StringBuilder finishedCode = new StringBuilder();
						scr.compile(new OuterCompileContext(finishedCode, getDebugRecorder().apply(ipc.typeSystem.codeGenFeatureLevel)));
						String errors = scr.diags.unwrapToString();
						if (errors != null) {
							sb.append("Compile errors:\n");
							sb.append(errors);
							sb.append("\n\nUnfinished code:\n");
						}
						sb.append(finishedCode.toString());
					} catch (Exception e) {
						Main.exceptionIntoSB(sb, e);
					}
					debugState.displayMessageToUser.fire(sb.toString());
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

	private Function<CodeGenFeatureLevel, IDebugRecorder> getDebugRecorder() {
		return injectWithDebugInfo ? (cgfl) -> new FullDebugRecorder(cgfl) : (cgfl) -> new DummyDebugRecorder();
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
		if (debugState.doNotInject()) {
			debugState.displayMessageToUser.fire("Cannot inject: busy (open debugger!)");
			return;
		}
		StringBuilder sb = new StringBuilder();
		if (currentFile == null) {
			sb.append("No file!");
		} else {
			if (Main.inject(sb, stdLibDP, currentFile, getDebugRecorder(), (ts) -> {
				debugState.debugTaxonomy = new DebugTaxonomyData(ts);
			}, sections)) {
				sb.append("\nInject successful.");
			} else {
				sb.append("\nInject failed.");
			}
		}
		debugState.displayMessageToUser.fire(sb.toString());
	}
}
