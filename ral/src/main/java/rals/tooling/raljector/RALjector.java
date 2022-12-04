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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

import rals.Main;
import rals.code.ScriptSection;
import rals.parser.IDocPath;

/**
 * GUI tool for injection and stuff.
 */
public class RALjector {
	public static void run(final IDocPath stdLibDP) {
		final JButton[] mainButtons = new JButton[6];
		final String[] mainButtonTexts = {
				"File: NONE",
				"Collapse Buttons",
				"Inject Install/Events",
				"Install",
				"Events",
				"Remove"
		};
		final String[] smallButtonTexts = {
				"FIL",
				"Xpd",
				"I/E",
				"I",
				"E",
				"R"
		};
		final JFrame jf = new JFrame("RALjector");
		final JDialog injectFrame = new JDialog(jf, "Inject");
		injectFrame.setSize(400, 300);
		final JTextPane injectTextArea = new JTextPane();
		injectTextArea.setEditable(false);
		injectFrame.setContentPane(new JScrollPane(injectTextArea));
		final AtomicBoolean isSmall = new AtomicBoolean(false);
		final Runnable updateTexts = () -> {
			boolean nowSmall = isSmall.get();
			for (int i = 0; i < mainButtons.length; i++)
				mainButtons[i].setText(nowSmall ? smallButtonTexts[i] : mainButtonTexts[i]);
		};
		final AtomicReference<File> fileRef = new AtomicReference<>();
		final Runnable[] actions = new Runnable[] {
				() -> {
					FileDialog fd = new FileDialog(jf);
					fd.setFile("*.ral");
					fd.setVisible(true);
					File[] fs = fd.getFiles();
					if (fs.length == 1) {
						fileRef.set(fs[0]);
						mainButtonTexts[0] = "File: " + fs[0].getName();
						updateTexts.run();
					}
				},
				() -> {
					isSmall.set(!isSmall.get());
					updateTexts.run();
					jf.pack();
				},
				() -> {
					injectUI(injectFrame, injectTextArea, stdLibDP, fileRef, ScriptSection.Events, ScriptSection.Install);
				},
				() -> {
					injectUI(injectFrame, injectTextArea, stdLibDP, fileRef, ScriptSection.Install);
				},
				() -> {
					injectUI(injectFrame, injectTextArea, stdLibDP, fileRef, ScriptSection.Events);
				},
				() -> {
					injectUI(injectFrame, injectTextArea, stdLibDP, fileRef, ScriptSection.Remove);
				},
		};
		jf.setTitle("RALjector");
		jf.setLayout(new GridLayout(0, 1));
		jf.setAlwaysOnTop(true);
		for (int i = 0; i < mainButtons.length; i++) {
			final int iF = i;
			mainButtons[i] = new JButton(mainButtonTexts[i]);
			mainButtons[i].addActionListener((a) -> {
				actions[iF].run();
			});
			jf.add(mainButtons[i]);
		}
		jf.pack();
		jf.setLocationByPlatform(true);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setVisible(true);
	}

	private static void injectUI(JDialog jf, JTextComponent text, IDocPath stdLibDP, AtomicReference<File> f, ScriptSection... sections) {
		File fRes = f.get();
		StringBuilder sb = new StringBuilder();
		if (fRes == null) {
			sb.append("No file!");
		} else {
			if (Main.inject(sb, stdLibDP, fRes, sections)) {
				sb.append("\nInject successful.");
			} else {
				sb.append("\nInject failed.");
			}
		}
		text.setText(sb.toString());
		jf.setVisible(true);
	}
}
