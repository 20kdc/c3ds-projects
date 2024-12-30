/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

import cdsp.common.data.skeleton.SkeletonDef;

/**
 * Contains specific world facts for the current target game.
 */
public interface VirtualCatalogue {
	/**
	 * Gets the SkeletonDef of this game.
	 */
	SkeletonDef getSkeletonDef();

	/**
	 * Finds a chemical's name (or null if unknown).
	 */
	String findChemName(int chem);

	/**
	 * Dummy VirtualCatalogue.
	 */
	public static final class Dummy implements VirtualCatalogue {
		private final SkeletonDef def;
		public Dummy(SkeletonDef sd) {
			def = sd;
		}
		@Override
		public SkeletonDef getSkeletonDef() {
			return def;
		}
		@Override
		public String findChemName(int chem) {
			return null;
		}
	}
}
