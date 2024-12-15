/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

import cdsp.common.data.skeleton.LoadedSkeleton;
import cdsp.common.s16.S16Image;
import cdsp.common.s16.Tint;
import cdsp.common.s16.TintedBufferedImageCache;

/**
 * The Norn viewer itself.
 */
@SuppressWarnings("serial")
public class JNorn extends JInfiniteCanvas {
	private Tint tint = new Tint();
	//private LoadedSkeleton loadedSkeleton = LoadedSkeleton.EMPTY;
	private TintedBufferedImageCache[] imageCache = new TintedBufferedImageCache[] { new TintedBufferedImageCache() };
	private Point[] partLocations = new Point[] { new Point(0, 0) };
	private int[] zOrder = new int[] { 0 };

	public JNorn() {
		
	}

	public void setParameters(LoadedSkeleton skeleton, int[] partFrames) {
		if (skeleton.def.length != partFrames.length)
			throw new RuntimeException("Skeleton must have same amount of parts as frame array");
		//this.loadedSkeleton = skeleton;
		if (imageCache.length != partFrames.length) {
			imageCache = new TintedBufferedImageCache[partFrames.length];
			partLocations = new Point[partFrames.length];
			zOrder = new int[partFrames.length];
			for (int i = 0; i < partFrames.length; i++) {
				imageCache[i] = new TintedBufferedImageCache();
				imageCache[i].setTint(tint);
				partLocations[i] = new Point(0, 0);
			}
		}
		for (int i = 0; i < partFrames.length; i++)
			imageCache[i].setSource(skeleton.getPartImage(i, partFrames[i]));
		skeleton.updateSkeleton(partLocations, partFrames);
		skeleton.def.updateZOrder(partFrames, zOrder);
		// centre on the root part
		S16Image img = imageCache[0].getSource();
		if (img != null) {
			stageW = img.width;
			stageH = img.height;
		}
		repaintCleanly();
	}

	public void setTint(Tint tint) {
		this.tint = tint;
		for (int i = 0; i < imageCache.length; i++)
			imageCache[i].setTint(tint);
		repaintCleanly();
	}

	@Override
	public void paintStaticBackground(int w, int h, Graphics g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);
	}

	@Override
	public void paintStage(int w, int h, Graphics g) {
		for (int i = 0; i < zOrder.length; i++) {
			int pi = zOrder[i];
			BufferedImage bi = imageCache[pi].getImage();
			if (bi != null)
				g.drawImage(bi, partLocations[pi].x, partLocations[pi].y, null);
		}
	}
}
