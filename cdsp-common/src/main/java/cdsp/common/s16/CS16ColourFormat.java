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
    public static final CS16ColourFormat RGB565 = new CS16ColourFormat(5, 6, 5) {
        @Override
        public short to565(short src) {
            return src;
        }
    }; 
    public static final CS16ColourFormat RGB555 = new CS16ColourFormat(5, 5, 5) {
        @Override
        public short to565(short src) {
            int srcI = src & 0x7FFF;
            return (short) (
                    (srcI &  0x001F) |
                    ((srcI & 0x03E0) << 1) | ((srcI & 0x0200) >> 4) |
                    ((srcI & 0x7C00) << 1)
            );
        }
    };
    public static final CS16ColourFormat RGB5551 = new CS16ColourFormat(5, 5, 5) {
        @Override
        public short to565(short src) {
            return RGB555.to565((short) ((src >> 1) & 0x7FFF));
        }
    };

    /**
     * R/G/B bit counts for dithering.
     */
    public final int rBits, gBits, bBits;

    private CS16ColourFormat(int rb, int gb, int bb) {
        rBits = rb;
        gBits = gb;
        bBits = bb;
    }

    /**
     * Convert to RGB565.
     */
    public abstract short to565(short src);

    /**
     * Convert from RGB565 to ARGB8888.
     */
    public static int argbFrom565(short pix, boolean alphaAware) {
        int pixI = (short) pix;
        int argb =
        ((pixI & 0xF800) << 8) | ((pixI & 0xE000) << 3) |
        ((pixI & 0x07E0) << 5) | ((pixI & 0x0600) >> 1) |
        ((pixI & 0x001F) << 3) | ((pixI & 0x001C) >> 2);
        if (alphaAware && pixI != 0)
            argb |= 0xFF000000;
        return argb;
    }
}
