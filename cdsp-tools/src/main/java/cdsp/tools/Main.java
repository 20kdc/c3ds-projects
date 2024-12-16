/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.GridBagLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.app.GameInfo;
import cdsp.common.app.JButtonWR;
import cdsp.common.app.JGameInfo;
import cdsp.common.cpx.Injector;
import cdsp.common.data.DirLookup;
import cdsp.common.data.genetics.GenPackage;
import cdsp.common.data.genetics.GenUtils;
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
		setLayout(new GridBagLayout());
		JPanel configPanel = new JPanel();
		{
			configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
			configPanel.setLayout(new GridBagLayout());
			configPanel.add(new JButtonWR("Game Directories...", () -> {
				JDialog configPage = new JDialog(Main.this, "Game Directories");
				configPage.add(new JGameInfo(gameInfo));
				configPage.setSize(800, 600);
				configPage.setVisible(true);
			}));
			configPanel.add(new JButtonWR("CPX Info", () -> {
				try {
					StringBuilder info = new StringBuilder();
					info.append("Game information:\n");
					info.append("\tgame name: ");
					info.append(Injector.cpxRequest("execute\nouts gnam", gameInfo.charset));
					info.append("\n");
					info.append("\tengine version: ");
					info.append(Injector.cpxRequest("execute\noutv vmjr outs \".\" outv vmnr", gameInfo.charset));
					info.append("\n");
					info.append("\tengine modules: ");
					info.append(Injector.cpxRequest("execute\nouts modu", gameInfo.charset));
					info.append("\n");
					info.append("CPX extensions:\n");
					info.append("\tserver: ");
					info.append(Injector.cpxRequest("cpx-ver\n", gameInfo.charset));
					info.append("\n");
					info.append("\tgame path: ");
					info.append(Injector.cpxRequest("cpx-gamepath\n", Charset.defaultCharset()));
					info.append("\n");
					CDSPCommonUI.showReport("CPX Info", info.toString());
				} catch (Exception ex) {
					CDSPCommonUI.showExceptionDialog(Main.this, "Could not connect to CPX.\nCheck your CAOS proxy settings.", "Error", ex);
				}
			}));
		}
		JPanel geneticsPanel = new JPanel();
		{
			geneticsPanel.setBorder(BorderFactory.createTitledBorder("Genetics"));
			geneticsPanel.setLayout(new GridBagLayout());
			geneticsPanel.add(new JButtonWR("Summarize Genome", () -> {
				CDSPCommonUI.fileDialog(Main.this, "GEN...", FileDialog.LOAD, (f) -> {
					try {
						StringBuilder result = new StringBuilder();
						GenPackage gPackage = GenUtils.readGenome(f);
						result.append(gPackage.version.toString());
						result.append("\n");
						byte[] genomeData = gPackage.data;
						int offset = GenUtils.nextGene(genomeData, 0);
						while (offset < genomeData.length) {
							result.append(gPackage.version.summarizeGene(genomeData, offset));
							result.append("\n");
							offset = GenUtils.nextGene(genomeData, offset + 4);
						}
						CDSPCommonUI.showReport("Genome " + f + " Report", result.toString());
					} catch (Exception ex) {
						CDSPCommonUI.showExceptionDialog(Main.this, "Could not load genome.", "Error", ex);
					}
				});
			}), CDSPCommonUI.gridBagFill(0, 0, 1, 1, 0, 0));
			geneticsPanel.add(new JButtonWR("Norn Poser", () -> {
				if (!convinceUserToDoSetup())
					return;
				new NornPoser(gameInfo).setVisible(true);
			}), CDSPCommonUI.gridBagFill(0, 1, 1, 1, 0, 0));
		}
		JPanel imagingPanel = new JPanel();
		{
			imagingPanel.setBorder(BorderFactory.createTitledBorder("Imaging"));
			imagingPanel.setLayout(new GridBagLayout());
			imagingPanel.add(new JButtonWR("View C16/S16", () -> {
				CDSPCommonUI.fileDialog(Main.this, "C16/S16/BLK...", FileDialog.LOAD, (f) -> {
					new CS16Viewer(f, Main.this).setVisible(true);
				});
			}), CDSPCommonUI.gridBagFill(0, 0, 1, 1, 0, 0));
			imagingPanel.add(new JButtonWR("Convert To RGB565 (Rainbow Fix)", () -> {
				if (!convinceUserToDoSetup())
					return;
				converter(false, CS16Format.S16_RGB565, CS16Format.C16_RGB565);
			}), CDSPCommonUI.gridBagFill(0, 1, 1, 1, 0, 0));
			imagingPanel.add(new JButtonWR("Rewrite All As RGB565", () -> {
				if (!convinceUserToDoSetup())
					return;
				converter(true, CS16Format.S16_RGB565, CS16Format.C16_RGB565);
			}), CDSPCommonUI.gridBagFill(0, 2, 1, 1, 0, 0));
			imagingPanel.add(new JButtonWR("Rewrite All As RGB555 (Force Spew)", () -> {
				if (!convinceUserToDoSetup())
					return;
				converter(true, CS16Format.S16_RGB555, CS16Format.C16_RGB555);
			}), CDSPCommonUI.gridBagFill(0, 3, 1, 1, 0, 0));
		}
		add(configPanel, CDSPCommonUI.gridBagFill(0, 0, 1, 1, 1, 1));
		add(geneticsPanel, CDSPCommonUI.gridBagFill(0, 1, 1, 1, 1, 1));
		add(imagingPanel, CDSPCommonUI.gridBagFill(1, 0, 1, 2, 1, 1));
		pack();
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private boolean convinceUserToDoSetup() {
		String onboardingName = "cdsp-tools";
		if (gameInfo.looksEmpty()) {
			if (CDSPCommonUI.confirmInformationOperation(this, "It seems you haven't added your Docking Station directory yet!\nWould you like to do that now?\n(Pressing 'no' will allow you to proceed regardless, but you probably shouldn't do that.)", onboardingName)) {
				// if the user is running the game with the CAOS proxy on Windows, we can automatically determine the game path
				try {
					String result = Injector.cpxRequest("cpx-gamepath\n", Charset.defaultCharset());
					gameInfo.fromGameDirectory(new File(result.trim()));
					if (!gameInfo.looksEmpty())
						return true;
					// clean up bad entries
					gameInfo.reset();
				} catch (Exception ex) {
					// oh well
					System.err.println("CPX autodetect failed :(");
					ex.printStackTrace();
				}
				File file = CDSPCommonUI.selectDirectory(this);
				if (file != null) {
					gameInfo.reset();
					gameInfo.fromGameDirectory(file);
					if (!gameInfo.looksEmpty())
						return true;
				}
				return false;
			}
		}
		// everything is okay or user chose to proceed regardless.
		return true;
	}

	private void converter(boolean forceOverwrite, CS16Format uncompressed, CS16Format compressed) {
		LinkedList<File> locations = new LinkedList<>();
		locations.addAll(gameInfo.locations.get(DirLookup.Location.IMAGES));
		locations.addAll(gameInfo.locations.get(DirLookup.Location.BACKGROUNDS));
		StringWriter sw = new StringWriter();
		if (forceOverwrite)
			sw.append("*** warning: forceOverwrite enabled ***\n");
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
			conversionProgress.pack();
			conversionProgress.setLocationByPlatform(true);
			conversionProgress.setVisible(true);
			(new Thread() {
				@Override
				public void run() {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					try {
						int i = 0;
						int c = 0;
						for (File target : specificImages) {
							try {
								if (doConvert(target, false, forceOverwrite, uncompressed, compressed))
									c++;
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
								if (doConvert(target, true, forceOverwrite, uncompressed, compressed))
									c++;
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
						pw.append(Integer.toString(c));
						pw.append(" files changed\n");
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
		}
	}

	private static boolean doConvert(File img, boolean blk, boolean forceOverwrite, CS16Format uncompressed, CS16Format compressed) throws IOException {
		Path path = img.toPath();
		byte[] data = Files.readAllBytes(path);
		if (blk) {
			BLKInfo res = CS16IO.readBLKInfo(img);
			if (res.format == CS16Format.S16_RGB565 && !forceOverwrite)
				return false;
			ByteArrayOutputStream tmp = new ByteArrayOutputStream();
			CS16IO.encodeBLK(tmp, res, uncompressed);
			Files.write(path, tmp.toByteArray());
		} else {
			CS16Format originalFormat = CS16IO.determineFormat(data);
			CS16Format targetFormat = originalFormat.compressed ? compressed : uncompressed;
			if ((originalFormat == targetFormat) && !forceOverwrite)
				return false;
			S16Image[] res = CS16IO.decodeCS16(img);
			Files.write(path, CS16IO.encode(res, targetFormat));
		}
		return true;
	}

	public static void main(String[] args) throws IOException {
		CDSPCommonUI.fixAWT();
		new Main();
	}
}
