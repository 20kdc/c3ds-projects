/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * Just a JButton that picks a file
 */
@SuppressWarnings("serial")
public class JFilePicker extends JButton {
	public Consumer<File> onChangeFile = file -> {};
	private File f;
	public JFilePicker(File f, String dialogName) {
		setPreferredSize(new Dimension(16, getPreferredSize().height));
		setFile(f);
		addActionListener((a) -> {
			CDSPCommonUI.fileDialog((Frame) SwingUtilities.getWindowAncestor(JFilePicker.this), dialogName, FileDialog.LOAD, (file) -> {
				setFile(file);
				onChangeFile.accept(file);
			});
		});
	}
	public File getFile() {
		return f;
	}
	public void setFile(File f) {
		this.f = f;
		if (f == null) {
			setText("----");
		} else {
			setText(f.toString());
		}
	}
}
