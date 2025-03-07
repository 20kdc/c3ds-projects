/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

/**
 * Colour formats.
 */
public abstract class CS16ColourFormat {
	public static final CS16ColourFormat RGB565 = new CS16ColourFormat(5, 6, 5, (short) 0x0020) {
		@Override
		public short to565(short src) {
			return src;
		}
		@Override
		public short from565(short src, boolean alphaAware) {
			return src;
		}
	};
	public static final CS16ColourFormat RGB555 = new CS16ColourFormat(5, 5, 5, (short) 0x0001) {
		@Override
		public short to565(short src) {
			int srcI = src & 0x7FFF;
			return (short) ((srcI & 0x001F) | ((srcI & 0x03E0) << 1) | ((srcI & 0x0200) >> 4) | ((srcI & 0x7C00) << 1));
		}
		@Override
		public short from565(short src, boolean alphaAware) {
			if (src == 0)
				return 0;
			int r = (src >> 11) & 0x1F;
			int g = (src >> 6) & 0x1F;
			int b = (src) & 0x1F;
			short v = (short) ((r << 10) | (g << 5) | b);
			if (alphaAware && v == 0)
				return nudge;
			return v;
		}
	};
	public static final CS16ColourFormat RGB5551 = new CS16ColourFormat(5, 5, 5, (short) 0x0002) {
		@Override
		public short to565(short src) {
			return RGB555.to565((short) ((src >> 1) & 0x7FFF));
		}
		@Override
		public short from565(short src, boolean alphaAware) {
			return (short) (RGB555.from565(src, alphaAware) << 1);
		}
	};

	/**
	 * R/G/B bit counts for dithering.
	 */
	public final int rBits, gBits, bBits;

	/**
	 * Non-transparent nudge colour.
	 */
	public final short nudge;

	private CS16ColourFormat(int rb, int gb, int bb, short nudge) {
		rBits = rb;
		gBits = gb;
		bBits = bb;
		this.nudge = nudge;
	}

	/**
	 * Convert to RGB565.
	 */
	public abstract short to565(short src);

	/**
	 * Convert from RGB565.
	 */
	public abstract short from565(short src, boolean alphaAware);

	/**
	 * Convert from RGB565 to ARGB8888.
	 */
	public static int argbFrom565(short pix, boolean alphaAware) {
		int pixI = (short) pix;
		int argb = ((pixI & 0xF800) << 8) | ((pixI & 0xE000) << 3) | ((pixI & 0x07E0) << 5) | ((pixI & 0x0600) >> 1)
				| ((pixI & 0x001F) << 3) | ((pixI & 0x001C) >> 2);
		if (alphaAware && pixI != 0)
			argb |= 0xFF000000;
		return argb;
	}
}
