/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.awt.image.BufferedImage;

/**
 * RGB565 image. Note that RGB555 is converted to RGB565 during load.
 */
public class S16Image {
    public final int width, height;
    public final short[] pixels;

    public S16Image(int w, int h) {
        width = w;
        height = h;
        pixels = new short[w * h];
    }

    public BufferedImage toBI(boolean alphaAware) {
        BufferedImage bi = new BufferedImage(width, height, alphaAware ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int[] intpix = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++)
            intpix[i] = CS16ColourFormat.argbFrom565(pixels[i], alphaAware);
        bi.getRaster().setDataElements(0, 0, width, height, intpix);
        return bi;
    }
}
