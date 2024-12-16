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
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdsp.common.data.skeleton.SkeletonDef;
import cdsp.common.data.skeleton.SkeletonDef.PoseChar;

/**
 * Editing poses so you don't have to.
 */
@SuppressWarnings("serial")
public class JPoseStringAndStateEditor extends JPanel {
	@SuppressWarnings("rawtypes")
	private final JComboBox[] poses;
	@SuppressWarnings("rawtypes")
	private final JComboBox[] states;
	private final PoseChar[] poseChars;

	public Runnable onChange = () -> {};

	private final SkeletonDef def;

	private final JTextField poseString;

	private final DocumentListener feedDoc = new DocumentListener() {
		@Override
		public void removeUpdate(DocumentEvent e) {
			setPoseString(poseString.getText());
			onChange.run();
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			setPoseString(poseString.getText());
			onChange.run();
		}
		@Override
		public void changedUpdate(DocumentEvent e) {
			setPoseString(poseString.getText());
			onChange.run();
		}
	};
	private final ItemListener feed = new RunnableItemListener(() -> {
		comboboxesToTextField();
		onChange.run();
	});

	public JPoseStringAndStateEditor(SkeletonDef def) {
		this.def = def;
		poseChars = def.getPoseChars();
		poseString = new JTextField(poseChars.length);
		setLayout(new GridBagLayout());
		poses = new JComboBox[poseChars.length];
		states = new JComboBox[poseChars.length];
		for (int i = 0; i < poseChars.length; i++) {
			add(new JLabel(poseChars[i].name, JLabel.CENTER), CDSPCommonUI.gridBagFill(0, i, 1, 1, 1, 1));
			char[] charValues = poseChars[i].getValues();
			Character[] values = new Character[charValues.length];
			for (int j = 0; j < values.length; j++)
				values[j] = charValues[j];
			poses[i] = new JComboBox<Character>(values);
			poses[i].setSelectedItem(poseChars[i].def);
			poses[i].addItemListener(feed);
			add(poses[i], CDSPCommonUI.gridBagFill(1, i, 1, 1, 1, 1));
			int associated = poseChars[i].associatedPartIndex;
			if (associated != -1) {
				states[i] = new JComboBox<String>(def.getPart(associated).getStates());
				states[i].addItemListener(feed);
				add(states[i], CDSPCommonUI.gridBagFill(2, i, 1, 1, 1, 1));
			}
		}
		add(poseString, CDSPCommonUI.gridBagFill(0, poseChars.length, 3, 1, 1, 0));
		poseString.setText(getPoseString());
		poseString.getDocument().addDocumentListener(feedDoc);
	}

	public String getPoseString() {
		char[] chars = new char[poses.length];
		for (int i = 0; i < chars.length; i++)
			chars[i] = (Character) poses[i].getSelectedItem();
		return new String(chars);
	}

	private void comboboxesToTextField() {
		poseString.getDocument().removeDocumentListener(feedDoc);
		poseString.setText(getPoseString());
		poseString.getDocument().addDocumentListener(feedDoc);
	}

	public void setPoseString(String pose) {
		for (int i = 0; i < poses.length; i++) {
			if (i >= pose.length())
				break;
			char chr = pose.charAt(i);
			if (poseChars[i].hasValue(chr)) {
				poses[i].removeItemListener(feed);
				poses[i].setSelectedItem(chr);
				poses[i].addItemListener(feed);
			}
		}
		comboboxesToTextField();
	}

	public void intoPartFrames(int[] partFrames) {
		def.decodePoseString(partFrames, getPoseString());
		for (int i = 0; i < poseChars.length; i++)
			if (states[i] != null)
				def.setState(partFrames, poseChars[i].associatedPartIndex, states[i].getSelectedIndex());
	}
}
