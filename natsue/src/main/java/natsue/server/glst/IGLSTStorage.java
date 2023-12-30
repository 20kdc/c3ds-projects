/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.glst;

/**
 * Storage for archived GLST chunks
 */
public interface IGLSTStorage {
	/**
	 * Stores GLST data. May decompress it, etcetc. first, but that's all internals.
	 */
	void storeGLST(String moniker, byte[] data);
}
