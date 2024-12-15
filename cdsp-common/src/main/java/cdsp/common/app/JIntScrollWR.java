/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

/**
 * To be honest, I don't know what you expected.
 */
@SuppressWarnings("serial")
public class JIntScrollWR extends JPanel {
	private final JLabel text;
	private final JScrollBar scrollBar;
	public Runnable onChange = () -> {};

	public JIntScrollWR(int value, int min, int max) {
		setLayout(new GridBagLayout());
		text = new JLabel("00000000");
		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, value, 1, min, max);
		text.setLabelFor(scrollBar);
		text.setHorizontalAlignment(JLabel.CENTER);
		text.setVerticalAlignment(JLabel.CENTER);
		text.setPreferredSize(new Dimension(64, 24));
		text.setText(Integer.toString(value) + "/" + Integer.toString(max));
		add(text, CDSPCommonUI.gridBagFill(0, 0, 1, 1, 0, 1));
		add(scrollBar, CDSPCommonUI.gridBagFill(1, 0, 1, 1, 1, 1));
		scrollBar.addAdjustmentListener(adj -> {
			text.setText(Integer.toString(scrollBar.getValue()) + "/" + Integer.toString(max));
			onChange.run();
		});
	}

	public int getValue() {
		return scrollBar.getValue();
	}
	
	public void setValue(int v) {
		scrollBar.setValue(v);
	}
}
