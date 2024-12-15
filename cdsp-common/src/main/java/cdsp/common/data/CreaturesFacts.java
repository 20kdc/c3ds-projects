/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

/**
 * Universal(?) constants of Creatures games.
 */
public class CreaturesFacts {
	/**
	 * Genuses and their names.
	 */
	public static final String[] GENUS = new String[] {
		"Norn",
		"Grendel",
		"Ettin",
		"Geat"
	};
	/**
	 * Creatures 2 & C2e breed index count.
	 * Community Edition might change things, so we need to keep track of where we assume this.
	 */
	public static final String[] C_23_BREED_INDEX = new String[] {
		"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
		"n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
	};
	/**
	 * C2e age classifications
	 */
	public static final String[] C__3_AGES = new String[] {
		// the bounds here might be off, IDK
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	public static final String[] C123_SXS = new String[] {
		"Male", "Female"
		// :(
	};
}
