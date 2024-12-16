/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.function.Consumer;

import cdsp.common.data.IOUtils;
import cdsp.common.data.bytestring.W1252Fixed;
import cdsp.common.data.genetics.GenPackage;
import cdsp.common.data.genetics.GenUtils;
import cdsp.common.data.pray.ExportedCreatures;
import cdsp.common.data.pray.PRAYBlock;
import cdsp.common.s16.BLKInfo;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

/**
 * Attempts to automatically figure out what's what.
 */
public class DoWhatIMeanLoader {
	/**
	 * Loads an image or returns an empty frameset.
	 */
	public static S16Image[] loadImage(File f, Frame frame) {
		if (f.getName().toLowerCase().endsWith(".blk")) {
			try {
				// .wine/drive_c/Program Files (x86)/Docking Station/Backgrounds/C2toDS.blk
				BLKInfo fr = CS16IO.readBLKInfo(f);
				System.out.println("BLK " + fr.width + " " + fr.height + " " + fr.dataOfs);
				return new S16Image[] {fr.decode()};
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(frame, "Could not load BLK.", "Error", ex);
			}
		} else {
			try {
				S16Image[] fr = CS16IO.decodeCS16(f);
				return fr;
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(frame, "Could not load C16/S16.", "Error", ex);
			}
		}
		return new S16Image[0];
	}

	/**
	 * Loads genetics or returns null.
	 */
	public static GenPackage loadGenetics(File f, Frame frame) {
		if (f.getName().toLowerCase().endsWith(".creature")) {
			try {
				byte[] data = Files.readAllBytes(f.toPath());
				LinkedList<PRAYBlock> blocks = PRAYBlock.read(IOUtils.wrapLE(data), Integer.MAX_VALUE, W1252Fixed.INSTANCE);
				PRAYBlock root = ExportedCreatures.findCreatureRootBlock(blocks);
				if (root == null)
					throw new RuntimeException("No creature root block");
				String expectedName = ExportedCreatures.monikerFromRootBlock(root) + "." + root.getType() + ".genetics";
				for (PRAYBlock pb : blocks)
					if (pb.getType().equals("GENE") && pb.getName().equals(expectedName))
						return GenUtils.readGenome(pb.data);
				throw new RuntimeException("GENE chunk '" + expectedName + "' is missing.");
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(frame, "Could not pull genetics from creature.", "Error", ex);
			}
		} else {
			try {
				return GenUtils.readGenome(f);
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(frame, "Could not load genetics.", "Error", ex);
			}
		}
		return null;
	}

	/**
	 * Ties everything up in a bow.
	 */
	public static void loadGeneticsFileDialog(Frame frame, FileDialogThenLoad<GenPackage> handler) {
		CDSPCommonUI.fileDialog(frame, "Genome/Exported Creature...", FileDialog.LOAD, (f) -> {
			GenPackage gPackage = loadGenetics(f, frame);
			if (gPackage == null)
				return;
			handler.accept(f, gPackage);
		});
	}

	public interface FileDialogThenLoad<T> {
		void accept(File from, T value);
	}
}
