/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

/**
 * Common dialogs and fixes.
 */
public class CDSPCommonUI {
	public static void showExceptionDialog(Component parent, String introText, String title, Exception exception) {
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		JOptionPane.showMessageDialog(parent, introText + "\n" + sw.toString(), title, JOptionPane.ERROR_MESSAGE);
	}

	public static boolean confirmDangerousOperation(Component parent, String text, String title) {
		int res = JOptionPane.showOptionDialog(parent, text, title, JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE, null, null, null);
		return res == JOptionPane.YES_OPTION;
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
