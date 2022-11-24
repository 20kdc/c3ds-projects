/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.HashMap;
import java.util.HashSet;

import rals.diag.SrcPosUntranslated;
import rals.lex.Token;

/**
 * Storage of HCM data (since HCM data on all active LSP files remains in memory at all times)
 */
public class HCMStorage {
	public final SrcPosMap<HCMScopeSnapshot> snapshots;
	public final SrcPosMap<Token> lastTokenMap;
	public final HashMap<Token.ID, HCMIntent> hoverIntents;
	public final HashMap<String, HoverData> allNamedTypes;
	public final HashMap<String, HoverData> allConstants;

	public HCMStorage(SrcPosMap<HCMScopeSnapshot> s, SrcPosMap<Token> tkn, HashMap<Token.ID, HCMIntent> id, HashMap<String, HoverData> ant, HashMap<String, HoverData> c) {
		snapshots = s;
		lastTokenMap = tkn;
		hoverIntents = id;
		allNamedTypes = ant;
		allConstants = c;
	}

	/**
	 * Gets hover information at a given point (or null for none)
	 */
	public HoverData getHoverData(SrcPosUntranslated tp) {
		Token tkn = lastTokenMap.get(tp);
		if (tkn == null) {
			return null;
		} else if (tkn instanceof Token.ID) {
			Token.ID id = (Token.ID) tkn;
			HCMIntent hi = hoverIntents.get(id);
			return hi.retrieve(tp, this).get(id.text);
		} else {
			return null;
		}
	}

	/**
	 * Hover data.
	 */
	public static final class HoverData {
		public final String text;
		public HoverData(String t) {
			text = t;
		}
	}
}
