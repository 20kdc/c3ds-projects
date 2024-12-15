/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.app.JInfiniteCanvas;
import cdsp.common.app.JIntScrollWR;
import cdsp.common.app.JTintEditor;
import cdsp.common.app.TintedBufferedImageCache;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

public class CS16Viewer {
	S16Image[] fr;
	TintedBufferedImageCache tbic = new TintedBufferedImageCache();
	int frI = 0;

	public CS16Viewer(S16Image[] fr) {
		this.fr = fr;
	}

	public static void main(String[] args) throws IOException {
		CDSPCommonUI.fixAWT();
		FileDialog fd = new FileDialog((JFrame) null);
		fd.setVisible(true);
		S16Image[] fr = CS16IO.decodeCS16(fd.getFiles()[0]);
		new CS16Viewer(fr).doTheThing();
	}

	@SuppressWarnings("serial")
	public void doTheThing() {
		JFrame testFrame = new JFrame();
		testFrame.setSize(800, 600);
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setResizeWeight(1);
		testFrame.add(jsp);
		JPanel left = new JPanel();
		JTintEditor right = new JTintEditor();
		right.copyFrom(tbic);
		jsp.add(left);
		jsp.add(right);
		left.setLayout(new GridBagLayout());
		JIntScrollWR scrollbar = new JIntScrollWR(frI, 0, fr.length);
		left.add(scrollbar, CDSPCommonUI.gridBagFill(0, 1, 1, 1, 1, 0));
		JInfiniteCanvas testCanvas = new JInfiniteCanvas() {
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
		right.onEditTint = () -> {
			tbic.copyFrom(right);
			refreshBI(testCanvas);
		};
		scrollbar.onChange = () -> {
			frI = scrollbar.getValue();
			refreshBI(testCanvas);
		};
		testFrame.setBackground(null);
		testCanvas.setBackground(null);
		left.add(testCanvas, CDSPCommonUI.gridBagFill(0, 0, 1, 1, 1, 1));
		testFrame.setVisible(true);
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
