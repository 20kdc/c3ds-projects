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
import java.awt.Panel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;

import cdsp.common.app.CDSPCommonUI;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

public class CS16Viewer {
	S16Image[] fr;
	BufferedImage bi;
	int frI = 0;
	int ofsX = 0;
	int ofsY = 0;
	int lastX = 0;
	int lastY = 0;

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
		refreshBI();
		JFrame testFrame = new JFrame();
		testFrame.setSize(400, 400);
		Panel testCanvas = new Panel() {
			@Override
			public void paint(Graphics g) {
				doPaintTo(this, g);
			}
		};
		testFrame.setBackground(null);
		testCanvas.setBackground(null);
		testFrame.add(testCanvas);
		testFrame.setVisible(true);
		testCanvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					frI--;
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					frI++;
				}
				refreshBI();
				doPaintTo(testCanvas, testCanvas.getGraphics());
			}
		});
		testCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				lastX = e.getX();
				lastY = e.getY();
			}
		});
		testCanvas.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				int ex = e.getX();
				int ey = e.getY();
				ofsX += ex - lastX;
				ofsY += ey - lastY;
				lastX = ex;
				lastY = ey;
				doPaintTo(testCanvas, testCanvas.getGraphics());
			}
		});
	}

	private void refreshBI() {
		if (frI < 0 || frI >= fr.length) {
			bi = null;
		} else {
			bi = fr[frI].toBI(true);
		}
	}

	private void doPaintTo(Panel p, Graphics g) {
		int w = p.getWidth();
		int h = p.getHeight();
		BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics tmpG = tmp.getGraphics();
		tmpG.setColor(Color.black);
		tmpG.fillRect(0, 0, w, h);
		tmpG.setColor(Color.white);
		tmpG.drawString(Integer.toString(frI), 8, 16);
		if (bi != null)
			tmpG.drawImage(bi, ofsX, ofsY, null);
		tmpG.setColor(Color.black);
		tmpG.fillRect(0, 0, 128, 24);
		tmpG.setColor(Color.white);
		tmpG.drawString(Integer.toString(frI), 8, 16);
		g.drawImage(tmp, 0, 0, null);
	}
}
