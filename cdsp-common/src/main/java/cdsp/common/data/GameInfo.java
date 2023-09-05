/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Game information.
 * Currently mostly a stub.
 */
public class GameInfo implements DirLookup {
    /**
     * Character set of the game.
     */
    public final Charset charset;

    private final File file;

    /**
     * Initializes GameInfo from machine.cfg location.
     * This might have to be refactored in future because C3/CA don't have machine.cfg
     */
    public GameInfo(File machineCfg, Charset charset) {
        this.file = machineCfg;
        this.charset = charset;
    }

    @Override
    public File findFile(Location location, String name) {
        return new File(new File(file.getParentFile(), location.nameTypical), name);
    }
}
