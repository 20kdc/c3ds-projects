/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class JGameInfoLocation extends JPanel {
	// this is a bit nasty but helps to make selection less painful
	private static File lastDir = null;
	private final JList<File> jlf;
	private final LinkedList<File> files;

	public JGameInfoLocation(Component d, LinkedList<File> files, Runnable onModify) {
		this.files = files;
		jlf = new JList<>(ftar(files));
		setLayout(new BorderLayout());
		add(jlf, BorderLayout.CENTER);
		JPanel jbp = new JPanel();
		jbp.setLayout(new BoxLayout(jbp, BoxLayout.X_AXIS));
		jbp.add(new JButtonWR("Add", () -> {
			File f = selectDirectory(d);
			if (f != null) {
				files.addLast(f);
				jlf.setListData(ftar(files));
				onModify.run();
			}
		}));
		jbp.add(new JButtonWR("Remove", () -> {
			File f = jlf.getSelectedValue();
			if (f != null) {
				files.remove(f);
				jlf.setListData(ftar(files));
				onModify.run();
			}
		}));
		jbp.add(new JButtonWR("Move To Top", () -> {
			File f = jlf.getSelectedValue();
			if (f != null) {
				files.remove(f);
				files.add(f);
				jlf.setListData(ftar(files));
				onModify.run();
			}
		}));
		jbp.add(new JLabel(" Directories are searched in order. "));
		add(jbp, BorderLayout.SOUTH);
	}

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

	private File[] ftar(LinkedList<File> files) {
		ArrayList<File> al = new ArrayList<>(files);
		return al.toArray(new File[0]);
	}

	public void refreshDirs() {
		jlf.setListData(ftar(files));
	}
}
