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
import rals.expr.RALExprSlice;
import rals.hcm.HCMStorage.HoverData;
import rals.lex.Token;

/**
 * All intent subclasses
 */
public class HCMIntents {
	/**
	 * Ambiguous ID.
	 */
	public static final HCMIntent ID = new HCMIntent() {
		/**
		 * This mirrors RALAmbiguousID.
		 */
		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			HashMap<String, HoverData> map = new HashMap<>();
			HCMScopeSnapshot snapshot = storage.snapshots.get(spu);
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
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return storage.allNamedTypes;
		}
	};

	/**
	 * Macro call.
	 */
	public static final HCMIntent CALLABLE = new HCMIntent() {
		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return storage.allCallables;
		}
	};

	/**
	 * Macro call, relative to arguments
	 */
	public static final HCMRelativeIntent CALLABLE_ARGS = new HCMRelativeIntent(1, CALLABLE) {
		@Override
		public Map<String, HoverData> retrieveParameterized(Token sp, SrcPosUntranslated spu, HCMStorage storage, RALExprSlice[] exprs) {
			return storage.allCallablesAV.get(exprs[0].length);
		}
	};
}
