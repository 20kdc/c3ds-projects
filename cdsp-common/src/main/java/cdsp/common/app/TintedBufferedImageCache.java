/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.awt.image.BufferedImage;

import cdsp.common.s16.S16Image;

/**
 * Caches a tinted frame.
 */
public class TintedBufferedImageCache implements TintHolder {
	private S16Image lastImage;
	private BufferedImage lastBI;
	private int tintR = 128, tintG = 128, tintB = 128, tintRot = 128, tintSwap = 128;

	@Override
	public int getTintR() {
		return tintR;
	}

	@Override
	public int getTintG() {
		return tintG;
	}

	@Override
	public int getTintB() {
		return tintB;
	}

	@Override
	public int getTintRot() {
		return tintRot;
	}

	@Override
	public int getTintSwap() {
		return tintSwap;
	}

	@Override
	public void setTint(int r, int g, int b, int rot, int swap) {
		tintR = r;
		tintG = g;
		tintB = b;
		tintRot = rot;
		tintSwap = swap;
		lastBI = null;
	}

	public BufferedImage getImage() {
		if (lastBI != null)
			return lastBI;
		if (lastImage == null)
			return null;
		float red = (tintR - 128) / 255.0f;
		float green = (tintG - 128) / 255.0f;
		float blue = (tintB - 128) / 255.0f;
		float rotation = (tintRot - 128) / 128.0f;
		float swap = Math.abs(tintSwap - 128) / 128.0f;
		return lastBI = lastImage.toBITinted(true, red, green, blue, rotation, swap);
	}

	public void setSource(S16Image source) {
		lastImage = source;
		lastBI = null;
	}

	public S16Image getSource() {
		return lastImage;
	}
}
