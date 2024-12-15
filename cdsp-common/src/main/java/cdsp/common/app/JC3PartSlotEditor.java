/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.GridBagLayout;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdsp.common.data.CreaturesFacts;
import cdsp.common.data.skeleton.C3PartSlots;
import cdsp.common.data.skeleton.SkeletonDef;

/**
 * Edits C3 part slots.
 */
@SuppressWarnings("serial")
public class JC3PartSlotEditor extends JPanel {
	@SuppressWarnings("rawtypes")
	private final JComboBox[] genuses = new JComboBox[SkeletonDef.C3.geneCount];
	@SuppressWarnings("rawtypes")
	private final JComboBox[] breedSlots = new JComboBox[SkeletonDef.C3.geneCount];

	public Runnable onChange = () -> {};

	private final ItemListener feed = new RunnableItemListener(() -> onChange.run());

	public JC3PartSlotEditor() {
		setLayout(new GridBagLayout());
		for (int i = 0; i < genuses.length; i++) {
			add(new JLabel(SkeletonDef.C3.getGeneName(i), JLabel.CENTER), CDSPCommonUI.gridBagFill(0, i, 1, 1, 1, 1));
			genuses[i] = new JComboBox<String>(CreaturesFacts.GENUS);
			genuses[i].addItemListener(feed);
			add(genuses[i], CDSPCommonUI.gridBagFill(1, i, 1, 1, 1, 1));
			breedSlots[i] = new JComboBox<String>(CreaturesFacts.C_23_BREED_INDEX);
			breedSlots[i].addItemListener(feed);
			add(breedSlots[i], CDSPCommonUI.gridBagFill(2, i, 1, 1, 1, 1));
		}
	}

	public C3PartSlots getSlots() {
		int[] g = new int[genuses.length];
		int[] bs = new int[genuses.length];
		for (int i = 0; i < genuses.length; i++) {
			g[i] = genuses[i].getSelectedIndex();
			bs[i] = breedSlots[i].getSelectedIndex();
		}
		return new C3PartSlots(g, bs);
	}

	public void setSlots(C3PartSlots slots) {
		for (int i = 0; i < genuses.length; i++) {
			genuses[i].removeItemListener(feed);
			breedSlots[i].removeItemListener(feed);
			genuses[i].setSelectedIndex(slots.getGenus(i));
			breedSlots[i].setSelectedIndex(slots.getBreedSlot(i));
			genuses[i].addItemListener(feed);
			breedSlots[i].addItemListener(feed);
		}
	}
}
