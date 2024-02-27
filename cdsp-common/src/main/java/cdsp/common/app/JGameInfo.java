/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.BorderLayout;
import java.io.File;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import cdsp.common.data.DirLookup.Location;

@SuppressWarnings("serial")
public class JGameInfo extends JPanel {
	public JGameInfo(GameInfo gameInfo) {
		setLayout(new BorderLayout());
		LinkedList<JGameInfoLocation> jgils = new LinkedList<>();
		JTabbedPane directories = new JTabbedPane();
		for (Location loc : Location.values()) {
			JGameInfoLocation jgil = new JGameInfoLocation(this, gameInfo.locations.get(loc), () -> {
				gameInfo.saveToDefaultLocation();
			});
			jgils.add(jgil);
			directories.addTab(loc.nameInternal, jgil);
		}
		add(BorderLayout.CENTER, directories);
		JPanel buttonbar = new JPanel();
		buttonbar.setLayout(new BoxLayout(buttonbar, BoxLayout.X_AXIS));
		buttonbar.add(new JButtonWR("Add Game Directory", () -> {
			File res = JGameInfoLocation.selectDirectory(this);
			if (res != null) {
				gameInfo.fromGameDirectory(res);
				for (JGameInfoLocation jgil : jgils)
					jgil.refreshDirs();
			}
			gameInfo.saveToDefaultLocation();
		}));
		buttonbar.add(new JButtonWR("Clear All", () -> {
			for (LinkedList<File> llf : gameInfo.locations.values())
				llf.clear();
			for (JGameInfoLocation jgil : jgils)
				jgil.refreshDirs();
			gameInfo.saveToDefaultLocation();
		}));
		add(BorderLayout.SOUTH, buttonbar);
	}
}
