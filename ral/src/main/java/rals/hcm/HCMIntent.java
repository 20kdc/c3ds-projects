/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.HashMap;
import java.util.Map;

import rals.diag.SrcPosUntranslated;
import rals.hcm.HCMStorage.HoverData;

/**
 * Indicates the intent of a token.
 */
public abstract class HCMIntent {
	/**
	 * Ambiguous ID.
	 */
	public static final HCMIntent ID = new HCMIntent() {
		/**
		 * This mirrors RALAmbiguousID.
		 */
		@Override
		public Map<String, HoverData> retrieve(SrcPosUntranslated sp, HCMStorage storage) {
			HashMap<String, HoverData> map = new HashMap<>();
			HCMScopeSnapshot snapshot = storage.snapshots.get(sp);
			if (snapshot != null)
				map.putAll(snapshot.contents);
			map.putAll(storage.allConstants);
			return map;
		}
	};
	/**
	 * Type.
	 */
	public static final HCMIntent TYPE = new HCMIntent() {
		@Override
		public Map<String, HoverData> retrieve(SrcPosUntranslated sp, HCMStorage storage) {
			return storage.allNamedTypes;
		}
	};

	/**
	 * Retrieves a map.
	 * DO NOT WRITE TO THIS MAP.
	 */
	public abstract Map<String, HoverData> retrieve(SrcPosUntranslated sp, HCMStorage storage);
}
