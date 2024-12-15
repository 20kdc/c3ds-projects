/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.app.JFilePicker;
import cdsp.common.app.JInfiniteCanvas;
import cdsp.common.app.JIntScrollWR;
import cdsp.common.app.JTintEditor;
import cdsp.common.s16.BLKInfo;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;
import cdsp.common.s16.TintedBufferedImageCache;

@SuppressWarnings("serial")
public class CS16Viewer extends JFrame {
	S16Image[] fr = new S16Image[0];
	TintedBufferedImageCache tbic = new TintedBufferedImageCache();
	int frI = 0;
	private final JIntScrollWR scrollbar;
	private final JInfiniteCanvas testCanvas;
	private final JFilePicker filePicker = new JFilePicker(null, "C16/S16/BLK...");

	public CS16Viewer(File f, Frame frame) {
		this();
		setFileAndFrames(f, doLoad(f, frame));
	}

	private static S16Image[] doLoad(File f, Frame frame) {
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

	public CS16Viewer() {
		setSize(800, 600);
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setResizeWeight(1);
		add(jsp);
		JPanel left = new JPanel();
		JTintEditor right = new JTintEditor();
		right.setTint(tbic.getTint());
		jsp.setLeftComponent(left);
		jsp.setRightComponent(right);
		left.setLayout(new GridBagLayout());
		scrollbar = new JIntScrollWR(frI, 0, fr.length == 0 ? 1 : fr.length);
		left.add(filePicker, CDSPCommonUI.gridBagFill(0, 1, 1, 1, 1, 0));
		left.add(scrollbar, CDSPCommonUI.gridBagFill(0, 2, 1, 1, 1, 0));
		testCanvas = new JInfiniteCanvas() {
			@Override
			public void paintStaticBackground(int w, int h, Graphics g) {
				g.setColor(Color.black);
				g.fillRect(0, 0, w, h);
			}

			@Override
			public void paintStage(int w, int h, Graphics g) {
				BufferedImage bi = tbic.getImage();
				if (bi != null)
					g.drawImage(bi, 0, 0, null);
			}

			@Override
			public void paintStaticForeground(int w, int h, Graphics g) {
			}
		};
		refreshBI(testCanvas);
		filePicker.onChangeFile = (file) -> {
			setFile(file);
		};
		right.onEditTint = (tint) -> {
			tbic.setTint(tint);
			refreshBI(testCanvas);
		};
		scrollbar.onChange = () -> {
			frI = scrollbar.getValue();
			refreshBI(testCanvas);
		};
		setBackground(null);
		testCanvas.setBackground(null);
		left.add(testCanvas, CDSPCommonUI.gridBagFill(0, 0, 1, 1, 1, 1));
		setVisible(true);
	}

	public void setFile(File file) {
		setFileAndFrames(file, doLoad(file, CS16Viewer.this));
	}

	public void setFileAndFrames(File file, S16Image[] frames) {
		fr = frames;
		String name = file.toString();
		setTitle(name);
		frI = 0;
		scrollbar.setRange(0, 0, fr.length);
		filePicker.setFile(file);
		refreshBI(testCanvas);
	}

	public static void main(String[] args) throws IOException {
		CDSPCommonUI.fixAWT();
		new CS16Viewer().setVisible(true);
	}

	private void refreshBI(JInfiniteCanvas canvas) {
		if (frI < 0 || frI >= fr.length) {
			tbic.setSource(null);
		} else {
			S16Image frame = fr[frI];
			tbic.setSource(frame);
			canvas.stageW = frame.width;
			canvas.stageH = frame.height;
		}
		canvas.repaintCleanly();
	}
}
