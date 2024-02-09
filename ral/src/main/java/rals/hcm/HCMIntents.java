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
import rals.types.AgentInterface;
import rals.types.RALType;

/**
 * All intent subclasses
 */
public class HCMIntents {
	/**
	 * Ambiguous ID.
	 */
	public static final HCMIntent ID = new HCMIntent("ID") {
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
	public static final HCMIntent TYPE = new HCMIntent("TYPE") {
		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return storage.allNamedTypes;
		}
	};

	/**
	 * Macro call.
	 */
	public static final HCMIntent CALLABLE = new HCMIntent("CALLABLE") {
		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return storage.allCallables;
		}
	};

	/**
	 * Codegen level.
	 */
	public static final HCMIntent CODEGEN_LEVEL = new HCMIntent("CODEGEN_LEVEL") {
		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return HCMFixedMaps.CODEGEN_LEVELS;
		}
	};

	/**
	 * Codegen level.
	 */
	public static final HCMIntent DECLARATIONS = new HCMIntent("DECLARATIONS") {
		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return HCMFixedMaps.DECLARATIONS;
		}
	};

	/**
	 * Macro call, relative to arguments
	 */
	public static final HCMRelativeIntent CALLABLE_ARGS = new HCMRelativeIntent("CALLABLE_ARGS", 1, CALLABLE) {
		@Override
		public Map<String, HoverData> retrieveParameterized(Token sp, SrcPosUntranslated spu, HCMStorage storage, RALExprSlice[] exprs) {
			return storage.allCallablesAV.get(exprs[0].length);
		}
	};

	/**
	 * Field ID, relative to target agent
	 */
	public static final HCMRelativeIntent FIELD_EXPR = new HCMRelativeIntent("FIELD_EXPR", 1, null) {
		@Override
		public Map<String, HoverData> retrieveParameterized(Token sp, SrcPosUntranslated spu, HCMStorage storage, RALExprSlice[] exprs) {
			try {
				if (exprs[0].length == 1) {
					HashMap<String, HoverData> compiled = new HashMap<>();
					for (AgentInterface ai : exprs[0].slot(0).type.getInterfaces())
						for (String me : ai.fields.keySet())
							compiled.put(me, HCMHoverDataGenerators.fieldHoverData(ai, me));
					return compiled;
				}
			} catch (Exception ex) {
				// just in case - this stuff gets a bit spicy
			}
			return null;
		}
	};

	/**
	 * Message ID, relative to target agent
	 */
	public static final HCMRelativeIntent MESSAGE_EXPR = new MSExprRelativeIntent("MESSAGE_EXPR", false);

	/**
	 * Script ID, relative to target agent
	 */
	public static final HCMRelativeIntent SCRIPT_EXPR = new MSExprRelativeIntent("SCRIPT_EXPR", true);

	public static HCMRelativeIntent msRelativeIntent(boolean asScript) {
		return asScript ? SCRIPT_EXPR : MESSAGE_EXPR;
	}

	private static final class MSExprRelativeIntent extends HCMRelativeIntent {
		public final boolean asScript;
		public MSExprRelativeIntent(String name, boolean s) {
			super(name, 1, null);
			asScript = s;
		}

		@Override
		public Map<String, HoverData> retrieveParameterized(Token sp, SrcPosUntranslated spu, HCMStorage storage, RALExprSlice[] exprs) {
			try {
				if (exprs[0].length == 1)
					return MSIntent.fromType(exprs[0].slot(0).type, asScript);
			} catch (Exception ex) {
				// just in case - this stuff gets a bit spicy
			}
			return null;
		}
	}

	public static final class MSIntent extends HCMIntent {
		public final boolean asScript;
		public final RALType type;
		public MSIntent(RALType rt, boolean s) {
			super((s ? "SCRIPT" : "MESSAGE") + "+" + rt);
			asScript = s;
			type = rt;
		}

		public static HashMap<String, HoverData> fromType(RALType type, boolean asScript) {
			try {
				HashMap<String, HoverData> compiled = new HashMap<>();
				for (AgentInterface ai : type.getInterfaces())
					for (Map.Entry<String, AgentInterface.MsgScr> me : (asScript ? ai.scripts : ai.messages).entrySet())
						compiled.put(me.getKey(), HCMHoverDataGenerators.msHoverData(ai, me.getKey(), me.getValue(), asScript));
				return compiled;
			} catch (Exception ex) {
				// just in case - this stuff gets a bit spicy
			}
			return null;
		}

		@Override
		public Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
			return fromType(type, asScript);
		}
	}

}
