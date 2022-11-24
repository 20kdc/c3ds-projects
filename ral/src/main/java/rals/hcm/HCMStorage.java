/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.HashSet;

import rals.diag.SrcPosUntranslated;
import rals.lex.Token;

/**
 * Storage of HCM data (since HCM data on all active LSP files remains in memory at all times)
 */
public class HCMStorage {
	public final SrcPosMap<HCMScopeSnapshot> snapshots;
	public final SrcPosMap<Token> lastTokenMap;
	public final HashSet<Token.ID> idReferences;

	public HCMStorage(SrcPosMap<HCMScopeSnapshot> s, SrcPosMap<Token> tkn, HashSet<Token.ID> id) {
		snapshots = s;
		lastTokenMap = tkn;
		idReferences = id;
	}

	/**
	 * Gets hover information at a given point (or null for none)
	 */
	public HoverData getHoverData(SrcPosUntranslated tp) {
		Token tkn = lastTokenMap.get(tp);
		if (tkn == null)
			return null;
		if (idReferences.contains(tkn)) {
			Token.ID id = (Token.ID) tkn;
			HCMScopeSnapshot hss = snapshots.get(tp);
			if (hss == null)
				return null;
			return hss.contents.get(id.text);
		}
		return null;
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
