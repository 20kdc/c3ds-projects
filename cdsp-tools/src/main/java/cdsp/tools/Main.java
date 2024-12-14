/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.app.GameInfo;
import cdsp.common.app.JButtonWR;
import cdsp.common.app.JGameInfo;
import cdsp.common.data.DirLookup;
import cdsp.common.s16.BLKInfo;
import cdsp.common.s16.CS16Format;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

@SuppressWarnings("serial")
public class Main extends JFrame {
	public final GameInfo gameInfo = new GameInfo();

	public Main() {
		// Load config
		gameInfo.loadFromDefaultLocation();
		// Continue
		setTitle("cdsp-tools");
		setLayout(new GridLayout(0, 1));
		setAlwaysOnTop(true);
		add(new JButtonWR("Configuration", () -> {
			JDialog configPage = new JDialog(Main.this, "Configuration");
			configPage.add(new JGameInfo(gameInfo));
			configPage.setSize(800, 600);
			configPage.setVisible(true);
		}));
		add(new JButtonWR("View C16/S16", () -> {
			FileDialog fd = new FileDialog((JFrame) null);
			fd.setMultipleMode(false);
			fd.setVisible(true);
			File[] files = fd.getFiles();
			if (files.length == 1) {
				File f = files[0];
				if (f.getName().toLowerCase().endsWith(".blk")) {
					try {
						// .wine/drive_c/Program Files (x86)/Docking Station/Backgrounds/C2toDS.blk
						BLKInfo fr = CS16IO.readBLKInfo(f);
						System.out.println("BLK " + fr.width + " " + fr.height + " " + fr.dataOfs);
						new CS16Viewer(new S16Image[] {fr.decode()}).doTheThing();
					} catch (Exception ex) {
						CDSPCommonUI.showExceptionDialog(Main.this, "Could not load BLK.", "Error", ex);
					}
				} else {
					try {
						S16Image[] fr = CS16IO.decodeCS16(f);
						new CS16Viewer(fr).doTheThing();
					} catch (Exception ex) {
						CDSPCommonUI.showExceptionDialog(Main.this, "Could not load C16/S16.", "Error", ex);
					}
				}
			}
		}));
		add(new JButtonWR("Convert To RGB565", () -> {
			LinkedList<File> locations = new LinkedList<>();
			locations.addAll(gameInfo.locations.get(DirLookup.Location.IMAGES));
			locations.addAll(gameInfo.locations.get(DirLookup.Location.BACKGROUNDS));
			StringWriter sw = new StringWriter();
			sw.append("This will overwrite C16, S16 and BLK files in these game directories:\n");
			for (File f : locations)
				sw.append(f + "\n");
			if (CDSPCommonUI.confirmDangerousOperation(Main.this, sw.toString(), "Warning!")) {
				LinkedList<File> specificImages = new LinkedList<>();
				LinkedList<File> specificBackgrounds = new LinkedList<>();
				for (File f : locations) {
					File[] parts = f.listFiles();
					if (parts != null) {
						for (File target : parts) {
							if (!target.isFile())
								continue;
							String name = target.getName().toLowerCase();
							if (name.endsWith(".c16") || name.endsWith(".s16"))
								specificImages.add(target);
							if (name.endsWith(".blk"))
								specificBackgrounds.add(target);
						}
					}
				}
				JFrame conversionProgress = new JFrame("Converting...");
				JProgressBar progress = new JProgressBar();
				progress.setMaximum(specificImages.size() + specificBackgrounds.size());
				conversionProgress.add(progress);
				(new Thread() {
					@Override
					public void run() {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						try {
							int i = 0;
							for (File target : specificImages) {
								try {
									doConvert(target, false);
								} catch (Exception ex) {
									pw.append("Error in sprite: ");
									pw.append(target.toString());
									pw.append("\n");
									ex.printStackTrace();
									ex.printStackTrace(pw);
								}
								i++;
								final int i2 = i;
								EventQueue.invokeLater(() -> {
									progress.setValue(i2);
								});
							}
							for (File target : specificBackgrounds) {
								try {
									doConvert(target, true);
								} catch (Exception ex) {
									pw.append("Error in background: ");
									pw.append(target.toString());
									pw.append("\n");
									ex.printStackTrace();
									ex.printStackTrace(pw);
								}
								i++;
								final int i2 = i;
								EventQueue.invokeLater(() -> {
									progress.setValue(i2);
								});
							}
							pw.append(Integer.toString(i));
							pw.append(" files processed\n");
						} catch (Exception ex) {
							pw.append("\nCritical exception\n");
							ex.printStackTrace();
							ex.printStackTrace(pw);
						} finally {
							EventQueue.invokeLater(() -> {
								conversionProgress.setVisible(false);
								JOptionPane.showMessageDialog(Main.this, sw.toString());
							});
						}
					}
				}).start();
				conversionProgress.setVisible(true);
			}
		}));
		pack();
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private static void doConvert(File img, boolean blk) throws IOException {
		Path path = img.toPath();
		byte[] data = Files.readAllBytes(path);
		if (blk) {
			BLKInfo res = CS16IO.readBLKInfo(img);
			ByteArrayOutputStream tmp = new ByteArrayOutputStream();
			CS16IO.encodeBLK(tmp, res);
			Files.write(path, tmp.toByteArray());
		} else {
			CS16Format originalFormat = CS16IO.determineFormat(data);
			S16Image[] res = CS16IO.decodeCS16(img);
			if (originalFormat.compressed) {
				Files.write(path, CS16IO.encodeC16(res));
			} else {
				Files.write(path, CS16IO.encodeS16(res));
			}
		}
	}

	public static void main(String[] args) throws IOException {
		CDSPCommonUI.fixAWT();
		new Main();
	}
}
