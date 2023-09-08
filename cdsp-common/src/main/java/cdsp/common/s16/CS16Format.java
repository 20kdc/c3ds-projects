/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

import java.nio.ByteOrder;

/**
 * Description of a C16/S16 format.
 */
public enum CS16Format {
    S16_RGB555(0x00000000, false, CS16ColourFormat.RGB555, ByteOrder.LITTLE_ENDIAN, "S16 RGB555 LE"),
    S16_RGB565(0x00000001, false, CS16ColourFormat.RGB565, ByteOrder.LITTLE_ENDIAN, "S16 RGB565 LE"),
    C16_RGB555(0x00000002, true, CS16ColourFormat.RGB555, ByteOrder.LITTLE_ENDIAN, "C16 RGB555 LE"),
    C16_RGB565(0x00000003, true, CS16ColourFormat.RGB565, ByteOrder.LITTLE_ENDIAN, "C16 RGB565 LE"),
    N16(0x01000000, false, CS16ColourFormat.RGB5551, ByteOrder.BIG_ENDIAN, "N16 RGB5551 BE"),
    M16(0x03000000, false, CS16ColourFormat.RGB5551, ByteOrder.BIG_ENDIAN, "M16 RGB5551 BE");

    /**
     * Magic number
     */
    public final int magic;
    /**
     * C16 compression
     */
    public final boolean compressed;
    /**
     * Colour format
     */
    public final CS16ColourFormat colourFormat;
    /**
     * Endian
     */
    public final ByteOrder endian;
    /**
     * Description
     */
    public final String description;

    CS16Format(int id, boolean compressed, CS16ColourFormat cfmt, ByteOrder endian, String desc) {
        this.magic = id;
        this.compressed = compressed;
        this.colourFormat = cfmt;
        this.endian = endian;
        this.description = desc;
    }
}
