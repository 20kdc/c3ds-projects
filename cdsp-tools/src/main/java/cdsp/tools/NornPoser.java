/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.app.DoWhatIMeanLoader;
import cdsp.common.app.JButtonWR;
import cdsp.common.app.JC3PartSlotEditor;
import cdsp.common.app.JFilePicker;
import cdsp.common.app.JNorn;
import cdsp.common.app.JPoseStringAndStateEditor;
import cdsp.common.app.JTintEditor;
import cdsp.common.app.RunnableItemListener;
import cdsp.common.data.CachedDirLookup;
import cdsp.common.data.CreaturesFacts;
import cdsp.common.data.DirLookup;
import cdsp.common.data.genetics.GenPackage;
import cdsp.common.data.skeleton.C3PartSlots;
import cdsp.common.data.skeleton.C3SkeletalAgingSimulation;
import cdsp.common.data.skeleton.LoadedSkeleton;
import cdsp.common.data.skeleton.SkeletonDef;
import cdsp.common.data.skeleton.SkeletonIndex;

/**
 * NornPoser!
 */
@SuppressWarnings("serial")
public class NornPoser extends JFrame {
	private LoadedSkeleton ls = LoadedSkeleton.EMPTY;
	private final DirLookup gameInfo;
	private final JNorn jNorn = new JNorn();
	private final JC3PartSlotEditor jpse = new JC3PartSlotEditor();
	private final JTintEditor tint = new JTintEditor();
	private final JFilePicker geneticsPicker = new JFilePicker(null, "Load genetics...");
	private final int[] pose;
	private final JComboBox<String> age = new JComboBox<>(CreaturesFacts.C__3_AGES);
	private final JComboBox<String> sxs = new JComboBox<>(CreaturesFacts.C123_SXS);
	private final JToggleButton reloadWithAge = new JToggleButton("Simulate");
	private final JPoseStringAndStateEditor poseString = new JPoseStringAndStateEditor(SkeletonDef.C3);

	private final ItemListener feedSimulator = new RunnableItemListener(() -> {
		if (reloadWithAge.isSelected()) {
			doLoadGenetics();
		} else {
			reloadSkeleton(this);
		}
	});

	public NornPoser(DirLookup gameInfo) {
		super("NornPoser");
		this.gameInfo = gameInfo;
		pose = new int[SkeletonDef.C3.length];
		setSize(1024, 600);
		setLocationByPlatform(true);

		reloadWithAge.setSelected(true);

		JPanel rhs = new JPanel();
		rhs.setLayout(new GridBagLayout());
		rhs.add(geneticsPicker, CDSPCommonUI.gridBagFill(0, 0, 3, 1, 1, 0));
		rhs.add(new JButtonWR("Reload", () -> doLoadGenetics()), CDSPCommonUI.gridBagFill(3, 0, 1, 1, 1, 0));
		rhs.add(jpse, CDSPCommonUI.gridBagFill(0, 1, 4, 1, 1, 1));
		rhs.add(tint, CDSPCommonUI.gridBagFill(0, 2, 4, 1, 1, 0));
		rhs.add(poseString, CDSPCommonUI.gridBagFill(4, 0, 4, 4, 1, 1));
		// bottom bar {
		rhs.add(age, CDSPCommonUI.gridBagFill(0, 3, 1, 1, 1, 0));
		rhs.add(sxs, CDSPCommonUI.gridBagFill(1, 3, 1, 1, 1, 0));
		rhs.add(reloadWithAge, CDSPCommonUI.gridBagFill(2, 3, 1, 1, 1, 0));
		rhs.add(new JButtonWR("Photo", () -> doPhoto()), CDSPCommonUI.gridBagFill(3, 3, 1, 1, 1, 0));
		// }
		for (int i = 0; i < pose.length; i++)
			pose[i] = 10;
		pose[1] = 9;
		reloadSkeleton(null);
		geneticsPicker.onChangeFile = file -> {
			doLoadGenetics();
		};
		jpse.onChange = () -> {
			reloadSkeleton(this);
		};
		tint.onEditTint = tint -> {
			jNorn.setTint(tint);
		};
		poseString.onChange = () -> {
			reloadSkeleton(this);
		};
		age.addItemListener(feedSimulator);
		sxs.addItemListener(feedSimulator);

		// final split
		JSplitPane jsp = new JSplitPane();
		jsp.setOpaque(false);
		jsp.setResizeWeight(1);
		add(jsp);

		jsp.setLeftComponent(jNorn);
		jsp.setRightComponent(rhs);
	}

	public void doLoadGenetics() {
		File f = geneticsPicker.getFile();
		if (f != null) {
			try {
				GenPackage genomeData = DoWhatIMeanLoader.loadGenetics(f, this);
				if (genomeData != null) {
					C3SkeletalAgingSimulation simulator = new C3SkeletalAgingSimulation(genomeData);
					for (int i = 0; i <= age.getSelectedIndex(); i++)
						simulator.executeAge(i, sxs.getSelectedIndex());
					tint.setTint(simulator.myTint);
					jNorn.setTint(simulator.myTint);
					jpse.setSlots(simulator.myPartSlots);
				}
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(this, "Could not simulate genetics.", "Error", ex);
				geneticsPicker.setFile(null);
			}
		}
		reloadSkeleton(this);
	}

	public void doPhoto() {
		CDSPCommonUI.fileDialog(this, "Save PNG...", FileDialog.SAVE, file -> {
			try {
				BufferedImage bi = jNorn.createPhoto();
				ImageIO.write(bi, "PNG", file);
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(this, "Could not save image.", "Error", ex);
			}
		});
	}

	public void reloadSkeleton(Frame frame) {
		try {
			poseString.intoPartFrames(pose);
			C3PartSlots c3ps = jpse.getSlots();
			SkeletonIndex[] genes = c3ps.asSkeletonIndexes(sxs.getSelectedIndex(), age.getSelectedIndex());
			DirLookup cached = new CachedDirLookup(gameInfo);
			ls = new LoadedSkeleton(cached, genes, SkeletonDef.C3);
			jNorn.setParameters(ls, pose);
		} catch (Exception ex) {
			CDSPCommonUI.showExceptionDialog(frame, "Could not load skeleton.", "Error", ex);
		}
	}
}
