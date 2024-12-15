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
public class TintedBufferedImageCache {
	private S16Image lastImage;
	private BufferedImage lastBI;
	private Tint lastTint = new Tint();

	public Tint getTint() {
		return lastTint;
	}

	public void setTint(Tint tint) {
		if (lastTint != tint) {
			lastTint = tint;
			lastBI = null;
		}
	}

	public S16Image getSource() {
		return lastImage;
	}

	public void setSource(S16Image source) {
		if (lastImage != source) {
			lastImage = source;
			lastBI = null;
		}
	}

	public BufferedImage getImage() {
		if (lastBI != null)
			return lastBI;
		if (lastImage == null)
			return null;
		float red = (lastTint.r - 128) / 255.0f;
		float green = (lastTint.g - 128) / 255.0f;
		float blue = (lastTint.b - 128) / 255.0f;
		float rotation = (lastTint.rot - 128) / 128.0f;
		float swap = Math.abs(lastTint.swap - 128) / 128.0f;
		return lastBI = lastImage.toBITinted(true, red, green, blue, rotation, swap);
	}
}
