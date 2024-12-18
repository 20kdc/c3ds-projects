/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.awt.GridBagLayout;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import cdsp.common.s16.Tint;

/**
 * Edits tint.
 */
@SuppressWarnings("serial")
public class JTintEditor extends JPanel {
	/**
	 * Run on an internally generated edit to the tint settings.
	 */
	public Consumer<Tint> onEditTint = (tint) -> {};

	private final JIntScrollWR scR, scG, scB, scRot, scSwap;

	public JTintEditor() {
		Runnable generalOnChange = () -> {
			onEditTint.accept(getTint());
		};
		setLayout(new GridBagLayout());
		
		add(new JLabel("R"), CDSPCommonUI.gridBagFill(0, 0, 1, 1, 0, 0));
		scR = new JIntScrollWR(128, 0, 256);
		scR.onChange = generalOnChange;
		add(scR, CDSPCommonUI.gridBagFill(1, 0, 1, 1, 1, 0));

		add(new JLabel("G"), CDSPCommonUI.gridBagFill(0, 1, 1, 1, 0, 0));
		scG = new JIntScrollWR(128, 0, 256);
		scG.onChange = generalOnChange;
		add(scG, CDSPCommonUI.gridBagFill(1, 1, 1, 1, 1, 0));

		add(new JLabel("B"), CDSPCommonUI.gridBagFill(0, 2, 1, 1, 0, 0));
		scB = new JIntScrollWR(128, 0, 256);
		scB.onChange = generalOnChange;
		add(scB, CDSPCommonUI.gridBagFill(1, 2, 1, 1, 1, 0));
		
		add(new JLabel("Rot"), CDSPCommonUI.gridBagFill(0, 3, 1, 1, 0, 0));
		scRot = new JIntScrollWR(128, 0, 256);
		scRot.onChange = generalOnChange;
		add(scRot, CDSPCommonUI.gridBagFill(1, 3, 1, 1, 1, 0));
		
		add(new JLabel("Swap"), CDSPCommonUI.gridBagFill(0, 4, 1, 1, 0, 0));
		scSwap = new JIntScrollWR(128, 0, 256);
		scSwap.onChange = generalOnChange;
		add(scSwap, CDSPCommonUI.gridBagFill(1, 4, 1, 1, 1, 0));
	}

	public Tint getTint() {
		return new Tint(scR.getValue(), scG.getValue(), scB.getValue(), scRot.getValue(), scSwap.getValue());
	}

	public void setTint(Tint tint) {
		scR.setValue(tint.r);
		scG.setValue(tint.g);
		scB.setValue(tint.b);
		scRot.setValue(tint.rot);
		scSwap.setValue(tint.swap);
	}
}
