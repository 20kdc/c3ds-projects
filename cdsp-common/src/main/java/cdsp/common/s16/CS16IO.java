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
    /**
     * Autodetect CS16 format, then get frame info.
     */
    public static CS16FrameInfo[] readCS16FrameInfo(File file) throws IOException {
        return readCS16FrameInfo(Files.readAllBytes(file.toPath()));
    }

    /**
     * Autodetect CS16 format, then get frame info.
     */
    public static CS16FrameInfo[] readCS16FrameInfo(byte[] buffer) {
        return readCS16FrameInfo(buffer, 0xFFFF);
    }

    /**
     * Autodetect CS16 format, then get frame info.
     * This variant allows a maximum frame count, rather than the usual implicit 65535-frame bound.
     * This helps keep allocation under control.
     */
    public static CS16FrameInfo[] readCS16FrameInfo(byte[] buffer, int maxFrames) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int magic = bb.getInt(0);
        for (CS16Format fmt : CS16Format.values()) {
            if (fmt.magic != magic)
                continue;
            bb.order(fmt.endian);
            int count = bb.getShort(4) & 0xFFFF;
            if (count > maxFrames)
                throw new RuntimeException("Too many frames");
            CS16FrameInfo[] result = new CS16FrameInfo[count];
            int pos = 6;
            for (int i = 0; i < count; i++) {
                int dptr = bb.getInt(pos);
                int iw = bb.getShort(pos + 4) & 0xFFFF;
                int ih = bb.getShort(pos + 6) & 0xFFFF;
                if (fmt.compressed) {
                    pos += 4 * (ih + 1);
                } else {
                    pos += 8;
                }
                result[i] = new CS16FrameInfo(bb, fmt, iw, ih, dptr);
            }
            return result;
        }
        throw new RuntimeException("Unknown image type");
    }

    /**
     * Autodetect CS16 format, then get frame info and decode.
     */
    public static S16Image[] decodeCS16(File file) throws IOException {
        return decodeAll(readCS16FrameInfo(file));
    }

    /**
     * Autodetect CS16 format, then get frame info and decode.
     */
    public static S16Image[] decodeCS16(byte[] buffer) {
        return decodeAll(readCS16FrameInfo(buffer));
    }

    /**
     * Decode all CS16FrameInfos into S16Images.
     */
    public static S16Image[] decodeAll(CS16FrameInfo[] frames) {
        S16Image[] res = new S16Image[frames.length];
        for (int i = 0; i < frames.length; i++)
            res[i] = frames[i].decode();
        return res;
    }
}
