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
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.app.DoWhatIMeanLoader;
import cdsp.common.app.GameInfo;
import cdsp.common.app.JButtonWR;
import cdsp.common.app.JGameInfo;
import cdsp.common.cpx.Injector;
import cdsp.common.data.DirLookup;
import cdsp.common.data.DirLookup.Location;
import cdsp.common.data.genetics.GenPackage;
import cdsp.common.data.genetics.GenUtils;
import cdsp.common.s16.BLKInfo;
import cdsp.common.s16.CS16Format;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

@SuppressWarnings("serial")
public class Main extends JFrame {
	public static final String ONBOARDING_NAME = "cdsp-tools";
	public static final String EGG_REQUEST =
		"new: simp 3 4 1 \"eggs\" 8 56 2000\n" +
		"setv ov01 va00\n" +
		"elas 10 fric 100 attr 195 bhvr 32 aero 10 accg 4 perm 60\n" +
		"mvsf 500 9295\n" +
		"gene load targ 1 \"dave\"\n" +
		"cmrt 0";

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
				if (!convinceUserToInstallCPX())
					return;
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
					CDSPCommonUI.showExceptionDialog(Main.this, "Something weird happened.", "Error", ex);
				}
			}));
		}
		JPanel geneticsPanel = new JPanel();
		{
			geneticsPanel.setBorder(BorderFactory.createTitledBorder("Genetics"));
			geneticsPanel.setLayout(new GridBagLayout());
			geneticsPanel.add(new JButtonWR("Summarize Genome", () -> {
				DoWhatIMeanLoader.loadGeneticsFileDialog(this, (f, gPackage) -> {
					try {
						StringBuilder result = new StringBuilder();
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
						CDSPCommonUI.showExceptionDialog(Main.this, "Could not summarize genome.", "Error", ex);
					}
				});
			}), CDSPCommonUI.gridBagFill(0, 0, 1, 1, 0, 0));
			geneticsPanel.add(new JButtonWR("GeneCat", () -> {
				DoWhatIMeanLoader.loadGeneticsFileDialog(this, "Genome 1", (f1, gPackage1) -> {
					DoWhatIMeanLoader.loadGeneticsFileDialog(this, "Genome 2", (f2, gPackage2) -> {
						CDSPCommonUI.fileDialog(this, "Output", FileDialog.SAVE, (fo) -> {
							try {
								GenPackage res = gPackage1.cat(gPackage2);
								Files.write(fo.toPath(), res.toFileData());
							} catch (Exception ex) {
								CDSPCommonUI.showExceptionDialog(Main.this, "Could not concatenate genomes.", "Error", ex);
							}
						});
					});
				});
			}), CDSPCommonUI.gridBagFill(1, 0, 1, 1, 0, 0));
			geneticsPanel.add(new JButtonWR("Norn Poser", () -> {
				if (!convinceUserToDoSetup())
					return;
				new NornPoser(gameInfo).setVisible(true);
			}), CDSPCommonUI.gridBagFill(0, 1, 2, 1, 0, 0));
			geneticsPanel.add(new JButtonWR("Inject Egg (M)", () -> {
				eggject("1");
			}), CDSPCommonUI.gridBagFill(0, 2, 1, 1, 0, 0));
			geneticsPanel.add(new JButtonWR("Inject Egg (F)", () -> {
				eggject("2");
			}), CDSPCommonUI.gridBagFill(1, 2, 1, 1, 0, 0));
		}
		JPanel imagingPanel = new JPanel();
		{
			imagingPanel.setBorder(BorderFactory.createTitledBorder("Imaging"));
			imagingPanel.setLayout(new GridBagLayout());
			imagingPanel.add(new JButtonWR("View C16/S16/BLK", () -> {
				CDSPCommonUI.fileDialog(Main.this, "C16/S16/BLK...", FileDialog.LOAD, (f) -> {
					new CS16Viewer(f, Main.this).setVisible(true);
				});
			}), CDSPCommonUI.gridBagFill(0, 0, 1, 1, 0, 0));
			imagingPanel.add(new JButtonWR("Convert To RGB565 (Rainbow Fix)", () -> {
				converter(false, CS16Format.S16_RGB565, CS16Format.C16_RGB565);
			}), CDSPCommonUI.gridBagFill(0, 1, 1, 1, 0, 0));
			imagingPanel.add(new JButtonWR("Rewrite All As RGB565", () -> {
				converter(true, CS16Format.S16_RGB565, CS16Format.C16_RGB565);
			}), CDSPCommonUI.gridBagFill(0, 2, 1, 1, 0, 0));
			imagingPanel.add(new JButtonWR("Rewrite All As RGB555 (Force Spew)", () -> {
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
		if (gameInfo.looksEmpty()) {
			if (CDSPCommonUI.confirmInformationOperation(this, "It seems you haven't added your Docking Station directory yet!\nWould you like to do that now?\n(Pressing 'no' will allow you to proceed regardless, but you probably shouldn't do that.)", ONBOARDING_NAME)) {
				// if the user is running the game with the CAOS proxy on Windows, we can automatically determine the game path
				try {
					String result = Injector.cpxRequest("cpx-gamepath\n", Charset.defaultCharset());
					System.out.println("Autodetect: " + result);
					boolean windows = Injector.checkIfLikelyWindows();
					boolean windowsishPath = result.indexOf(':') == 1;
					if (windowsishPath && !windows) {
						result = result.replace('\\', '/');
						String home = System.getenv("HOME");
						if (home == null)
							home = ".";
						result = home + "/.wine/dosdevices/" + result.substring(0, 1).toLowerCase() + result.substring(1);
					} else if (windows && !windowsishPath) {
						result = "Z:" + result.replace('/', '\\');
					}
					System.out.println("Autodetect post-conversion: " + result);
					gameInfo.fromGameDirectory(new File(result));
					if (!gameInfo.looksEmpty()) {
						gameInfo.saveToDefaultLocation();
						return true;
					}
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
					if (!gameInfo.looksEmpty()) {
						gameInfo.saveToDefaultLocation();
						return true;
					}
				}
				return false;
			}
		}
		// everything is okay or user chose to proceed regardless.
		return true;
	}

	/**
	 * pretty-please?
	 */
	private boolean convinceUserToInstallCPX() {
		try {
			Injector.cpxRequest("cpx-ver\n", gameInfo.charset);
			// success, user has CPX installed & running
			return true;
		} catch (Injector.CPXException ex) {
			// if we get a CPXException, the user has CPX installed & running
			return true;
		} catch (Exception ex) {
			if (CDSPCommonUI.confirmInformationOperation(this, "CPX does not appear to be running.\nIf you require information about CPX, please press 'yes' now.", ONBOARDING_NAME)) {
				// user does not know what CPX is. let's introduce them
				JDialog dialog = new JDialog(this);
				dialog.setTitle("About CPX");
				JTextPane jtp = new JTextPane(new HTMLDocument());
				jtp.setEditable(false);
				jtp.setEditorKit(new HTMLEditorKit());
				// 'da spiel
				jtp.setText(
					"<h1>CPX: CAOS Proxy</h1>" +
					"<h2>What is it?</h2>" +
					"<p>Tools for Creatures games access the game via an interface.</p>" +
					"<p>For Creatures 1 and 2, it was DDE, but Creatures 3 and Docking Station instead use a 'shared memory interface'.</p>" +
					"<p>These interfaces require finicky platform-specific native code to access, and cannot traverse into and out of wrappers such as Wine easily.</p>" +
					"<p>This presents problems for people wishing to write cross-platform tools, and even for Windows-only applications, it limits the choice of languages. In particular, Java alone cannot access them.</p>" +
					"<h2>How do I install it?</h2>" +
					"<p>caosproxy/caosprox.exe is a program that (hopefully) should have been supplied with cdsp-tools, which can forward requests from most programming languages with TCP socket support to the game.</p>" +
					"<p>It also comes in an installable form, caosproxy/engine-cpx.dll ; this may be copied directly into the Docking Station directory to automatically start and close CPX with the game.</p>" +
					"<p>For Linux/Mac users running the game natively, python/cpxciesv.py implements CPX using the game's CAOS server.</p>" +
					"<h2>Where is it used?</h2>" +
					"<p>CPX was developed as the method of interfacing with Docking Station for tools in 20kdc/c3ds-projects, but it's available for anyone to use; a specification is included in caosproxy/spec.txt on how to communicate with it.</p>" +
					"<p>Within 20kdc/c3ds-projects, it is used for RALjector (a RAL injector and CAOS debugger) and for caosproxy/cpxinvrt.exe (allows connecting Creature Labs tools on Wine to connect to native Linux/Mac Docking Station).</p>"
				);
				dialog.setLayout(new GridBagLayout());
				JScrollPane jsp = new JScrollPane(jtp);
				jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				dialog.add(jsp, CDSPCommonUI.gridBagFill(0, 0, 1, 1, 1, 1));
				dialog.add(new JButtonWR("OK", () -> dialog.setVisible(false)), CDSPCommonUI.gridBagFill(0, 1, 1, 1, 1, 0));
				dialog.setSize(640, 480);
				dialog.setModal(true);
				dialog.setLocationByPlatform(true);
				SwingUtilities.invokeLater(() -> jsp.getVerticalScrollBar().setValue(0));
				dialog.setVisible(true);
				JOptionPane.showMessageDialog(this, "Press 'OK' once CPX and the game are running.\nIf caosprox.exe is used, there will be an icon in the notification tray.", ONBOARDING_NAME, JOptionPane.INFORMATION_MESSAGE);
				try {
					Injector.cpxRequest("cpx-ver\n", gameInfo.charset);
					// success, user has CPX installed & running
					return true;
				} catch (Injector.CPXException ex2) {
					// give an extra-nice diagnostic in this case
					CDSPCommonUI.showExceptionDialog(this, "CPX seems to be running, but communicating with the game failed.\nIf the game isn't running, you should start it and try again.", ONBOARDING_NAME, ex2);
					return false;
				} catch (Exception ex2) {
					CDSPCommonUI.showExceptionDialog(this, "It appears CPX is either not running or cannot be accessed.\nYou may need to confirm that a firewall is not restricting local TCP servers.", ONBOARDING_NAME, ex2);
					return false;
				}
			} else {
				// user knows what CPX is, so they'll get the error they get
				return true;
			}
		}
	}

	private void eggject(String sxs) {
		if (!convinceUserToDoSetup())
			return;
		if (!convinceUserToInstallCPX())
			return;
		DoWhatIMeanLoader.loadGeneticsFileDialog(this, (f, gPackage) -> {
			try {
				// standard genetics file name, essentially reserved for overwriting by GK/etc.
				File target = gameInfo.newFile(Location.GENETICS, "dave.gen");
				Files.write(target.toPath(), gPackage.toFileData());
				Injector.cpxRequest("execute\nsetv va00 " + sxs + " " + EGG_REQUEST, gameInfo.charset);
			} catch (Exception ex) {
				CDSPCommonUI.showExceptionDialog(Main.this, "Could not inject egg.", "Error", ex);
			}
		});
	}

	private void converter(boolean forceOverwrite, CS16Format uncompressed, CS16Format compressed) {
		if (!convinceUserToDoSetup())
			return;
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
