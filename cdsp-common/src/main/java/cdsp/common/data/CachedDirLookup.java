/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * During a skeleton rebuild, a lot of case insensitive file location queries are made.
 * These are pretty expensive, even 20 years later!
 */
public class CachedDirLookup implements DirLookup {
	private final EnumMap<Location, File[]> fileLists = new EnumMap<>(Location.class);
	private final EnumMap<Location, HashMap<String, File>> findMap = new EnumMap<>(Location.class);

	public final DirLookup base;

	public CachedDirLookup(DirLookup b) {
		base = b;
		for (Location l : Location.values()) {
			File[] fileArray = b.listFiles(l);
			fileLists.put(l, fileArray);
			HashMap<String, File> hm = new HashMap<>();
			for (File f : fileArray)
				hm.put(f.getName().toLowerCase(), f);
			findMap.put(l, hm);
		}
	}

	@Override
	public File findFile(Location location, String name) {
		File firstPass = findMap.get(location).get(name.toLowerCase());
		if (firstPass == null)
			return base.findFile(location, name);
		return firstPass;
	}

	@Override
	public File[] listFiles(Location location) {
		return fileLists.get(location).clone();
	}
}
