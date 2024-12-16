/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Common dialogs and fixes.
 */
public class CDSPCommonUI {
	// this is a bit nasty but helps to make selection less painful
	private static File lastDir = null;

	public static File selectDirectory(Component d) {
		JFileChooser fd = new JFileChooser(lastDir);
		fd.setFileHidingEnabled(false);
		fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int res = fd.showOpenDialog(d);
		lastDir = fd.getCurrentDirectory();
		if (res == JFileChooser.APPROVE_OPTION)
			return fd.getSelectedFile();
		return null;
	}

	public static void showExceptionDialog(Component parent, String introText, String title, Exception exception) {
		StringWriter sw = new StringWriter();
		exception.printStackTrace();
		exception.printStackTrace(new PrintWriter(sw));
		JOptionPane.showMessageDialog(parent, introText + "\n" + sw.toString(), title, JOptionPane.ERROR_MESSAGE);
	}

	public static boolean confirmInformationOperation(Component parent, String text, String title) {
		int res = JOptionPane.showOptionDialog(parent, text, title, JOptionPane.YES_NO_OPTION,
				JOptionPane.INFORMATION_MESSAGE, null, null, null);
		return res == JOptionPane.YES_OPTION;
	}

	public static boolean confirmDangerousOperation(Component parent, String text, String title) {
		int res = JOptionPane.showOptionDialog(parent, text, title, JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE, null, null, null);
		return res == JOptionPane.YES_OPTION;
	}

	public static void fileDialog(Frame parent, String title, int mode, Consumer<File> result) {
		FileDialog fd = new FileDialog(parent);
		try {
			fd.setDirectory(lastDir.getAbsolutePath());
		} catch (Exception ex) {
			// shhh.
		}
		fd.setMultipleMode(false);
		fd.setMode(mode);
		fd.setVisible(true);
		try {
			lastDir = new File(fd.getDirectory());
		} catch (Exception ex) {
			// shhh.
		}
		File[] files = fd.getFiles();
		if (files.length == 1) {
			File f = files[0];
			result.accept(f);
		}
	}

	public static void showReport(String title, String text) {
        Frame f = new Frame(title);
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            	f.dispose();
            }
        });
        TextArea ta = new TextArea();
        ta.setText(text);
        f.add(ta);
        f.setSize(800, 600);
        f.setVisible(true);
	}

	public static void fixAWT() {
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("sun.awt.erasebackgroundonresize", "true");
	}

	public static GridBagConstraints gridBagFill(int x, int y, int w, int h, float wX, float wY) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		gbc.weightx = wX;
		gbc.weighty = wY;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.CENTER;
		return gbc;
	}
}
