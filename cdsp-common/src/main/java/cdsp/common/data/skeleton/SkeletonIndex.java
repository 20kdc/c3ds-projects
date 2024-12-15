/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

import java.io.File;

import cdsp.common.data.DirLookup;
import cdsp.common.data.DirLookup.Location;

/**
 * Index in the skeleton fallback system.
 */
public abstract class SkeletonIndex {
	/**
	 * This skeleton index's suffix.
	 */
	public final String suffix;

	public SkeletonIndex(String sfx) {
		suffix = sfx;
	}

	/**
	 * Gets all possible combinations that ought to be attempted for this skeleton index.
	 */
	public abstract SkeletonIndex[] getSearchPath();

	public static final File findFileIn(Location location, DirLookup lookup, SkeletonIndex[] path, String prefix, String suffix) {
		for (SkeletonIndex si : path) {
			String name = prefix + si.suffix + suffix;
			//System.out.println(name);
			File res = lookup.findFile(location, name, true);
			if (res.exists())
				return res;
		}
		return null;
	}
}
