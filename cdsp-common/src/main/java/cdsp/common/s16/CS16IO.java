/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/**
 * IO
 */
public class CS16IO {
    public static S16Image[] decodeCS16(File file) throws IOException {
        return decodeCS16(Files.readAllBytes(file.toPath()));
    }

    public static S16Image[] decodeCS16(byte[] buffer) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int magic = bb.getInt(0);
        for (CS16Format fmt : CS16Format.values()) {
            if (fmt.magic != magic)
                continue;
            bb.order(fmt.endian);
            int count = bb.getShort(4) & 0xFFFF;
            S16Image[] result = new S16Image[count];
            int pos = 6;
            for (int i = 0; i < count; i++) {
                int dptr = bb.getInt(pos);
                int iw = bb.getShort(pos + 4) & 0xFFFF;
                int ih = bb.getShort(pos + 6) & 0xFFFF;
                if (fmt.compressed) {
                    pos += 4 * (ih + 1);
                    result[i] = readC16Frame(bb, iw, ih, dptr, fmt.colourFormat);
                } else {
                    pos += 8;
                    result[i] = readS16Frame(bb, iw, ih, dptr, fmt.colourFormat);
                }
            }
            return result;
        }
        throw new RuntimeException("Unknown image type");
    }

    private static S16Image readC16Frame(ByteBuffer bb, int iw, int ih, int pos, CS16ColourFormat colourFormat) {
        S16Image s16 = new S16Image(iw, ih);
        int pixIdx = 0;
        for (int row = 0; row < ih; row++) {
            while (true) {
                short elm = bb.getShort(pos);
                pos += 2;
                if (elm == 0)
                    break;
                int runLen = (elm >> 1) & 0x7FFF;
                if ((elm & 1) != 0) {
                    for (int idx = 0; idx < runLen; idx++) {
                        s16.pixels[pixIdx++] = colourFormat.to565(bb.getShort(pos));
                        pos += 2;
                    }
                } else {
                    pixIdx += runLen;
                }
            }
        }
        return s16;
    }

    private static S16Image readS16Frame(ByteBuffer bb, int iw, int ih, int pos, CS16ColourFormat colourFormat) {
        S16Image s16 = new S16Image(iw, ih);
        for (int i = 0; i < s16.pixels.length; i++) {
            s16.pixels[i] = colourFormat.to565(bb.getShort(pos));
            pos += 2;
        }
        return s16;
    }
}
