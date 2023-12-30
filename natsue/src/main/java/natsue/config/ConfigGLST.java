/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

import natsue.server.glst.GLSTStoreMode;

/**
 * GLST
 */
public class ConfigGLST extends BaseConfig.Group {
	/**
	 * GLST mode
	 */
	public final Emu<GLSTStoreMode> glstMode = new Emu<>("glstMode", GLSTStoreMode.none)
			.describe("Controls GLST (detailed history) storage. Options are: none (do not save), decompressed ('.bin', CreaturesArchive header removed, zlib decompressed), compressed ('.cra', with CreaturesArchive header)");

	/**
	 * GLST directory
	 */
	public final Str glstDir = new Str("glstDir", "glst")
			.describe("GLST storage.");
}
