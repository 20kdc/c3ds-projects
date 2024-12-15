/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/**
 * Infinite canvas.
 */
@SuppressWarnings("serial")
public abstract class JInfiniteCanvas extends Component {
	public int stageW = 0;
	public int stageH = 0;
	// floats because of higher scales
	public float offsetX = 0;
	public float offsetY = 0;
	private int lastX = 0;
	private int lastY = 0;
	private boolean mb1Down = false;
	private int scale = 1;

	public JInfiniteCanvas() {
		enableEvents(ComponentEvent.MOUSE_EVENT_MASK | ComponentEvent.MOUSE_MOTION_EVENT_MASK | ComponentEvent.MOUSE_WHEEL_EVENT_MASK);
	}

	@Override
	public final void paint(Graphics arg0) {
		int w = (getWidth() + (scale - 1)) / scale;
		int h = (getHeight() + (scale - 1)) / scale;
		if (w < 1)
			w = 1;
		if (h < 1)
			h = 1;
		BufferedImage bi = createContentsImage(w, h, true, true, true);
		arg0.drawImage(bi, 0, 0, bi.getWidth() * scale, bi.getHeight() * scale, null);
	}

	public void repaintCleanly() {
		Graphics g = getGraphics();
		if (g != null)
			paint(g);
	}

	public final BufferedImage createPhoto() {
		int w = (getWidth() + (scale - 1)) / scale;
		int h = (getHeight() + (scale - 1)) / scale;
		if (w < 1)
			w = 1;
		if (h < 1)
			h = 1;
		return createContentsImage(w, h, false, true, false);
	}

	public final BufferedImage createContentsImage(int w, int h, boolean background, boolean stage, boolean foreground) {
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics big = bi.getGraphics();
		int ofx = ((int) offsetX) + ((w - stageW) / 2);
		int ofy = ((int) offsetY) + ((h - stageH) / 2);
		if (background)
			paintStaticBackground(w, h, big);
		big.translate(ofx, ofy);
		if (stage)
			paintStage(w, h, big);
		big.translate(-ofx, -ofy);
		if (foreground)
			paintStaticForeground(w, h, big);
		return bi;
	}

	public abstract void paintStaticBackground(int w, int h, Graphics g);

	public abstract void paintStage(int w, int h, Graphics g);

	public void paintStaticForeground(int w, int h, Graphics g) {
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		super.processMouseMotionEvent(e);
		int x = e.getX();
		int y = e.getY();
		if (mb1Down) {
			offsetX += (x - lastX) / (float) scale;
			offsetY += (y - lastY) / (float) scale;
			repaintCleanly();
		}
		lastX = x;
		lastY = y;
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			lastX = e.getX();
			lastY = e.getY();
			if (e.getButton() != MouseEvent.BUTTON1) {
				offsetX = 0;
				offsetY = 0;
				repaintCleanly();
			} else {
				mb1Down = true;
			}
		} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			if (e.getButton() == MouseEvent.BUTTON1)
				mb1Down = false;
		}
	}

	@Override
	protected void processMouseWheelEvent(MouseWheelEvent e) {
		if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
			int amount = e.getWheelRotation();
			if (amount < 0)
				scale++;
			else if (amount > 0)
				scale--;
			if (scale < 1)
				scale = 1;
			if (scale > 8)
				scale = 8;
			repaintCleanly();
		}
	}
}
