/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import rals.diag.SrcPosUntranslated;
import rals.diag.SrcRange;
import rals.lex.Token;

/**
 * Storage of HCM data (since HCM data on all active LSP files remains in memory at all times)
 */
public class HCMStorage {
	public final SrcPosMap<HCMScopeSnapshot> snapshots;
	public final SrcPosMap<Token> lastTokenMap;
	public final HashMap<Token, Token> backwardsTokenLink;
	public final HashMap<Token.ID, HCMIntent> hoverIntents;
	public final HashMap<Token, HashSet<HCMIntent>> intentsOnNextToken;
	public final HashMap<String, HoverData> allNamedTypes;
	public final HashMap<String, HoverData> allConstants;
	public final HashMap<String, HoverData> allCallables;

	public HCMStorage(SrcPosMap<HCMScopeSnapshot> s, SrcPosMap<Token> tkn, HashMap<Token, Token> b, HashMap<Token.ID, HCMIntent> id, HashMap<Token, HashSet<HCMIntent>> ci, HashMap<String, HoverData> ant, HashMap<String, HoverData> c, HashMap<String, HoverData> m) {
		snapshots = s;
		lastTokenMap = tkn;
		backwardsTokenLink = b;
		hoverIntents = id;
		intentsOnNextToken = ci;
		allNamedTypes = ant;
		allConstants = c;
		allCallables = m;
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
			if (hi != null)
				return hi.retrieve(tp, this).get(id.text);
			return null;
		} else {
			return null;
		}
	}

	/**
	 * Gets completion information at a given point (or null for none)
	 */
	public Map<String, HoverData> getCompletion(SrcPosUntranslated spu) {
		// Cases we need to support:
		// tknA tknB<cursor> (i.e. let Cre<cursor> )
		// here, we wish to use tknA's next intent
		// tknB <cursor> (i.e. _ = <cursor>;)
		// here, we wish to use tknB's next intent
		// to tell which it is, we:
		// + determine if we're directly touching the token
		// + if we're touching it, check it's an ID!
		// The token on which a completion intent would be applied.
		Token refToken = lastTokenMap.get(spu);
		// The token we're currently writing, if any.
		// Token writingToken = null;
		if (refToken == null) {
			// we COULD guess the intent here because we're at the start of the file
			// but let's not.
			return null;
		}
		if ((refToken instanceof Token.ID) && (spu.lcLong <= refToken.extent.end.lcLong)) {
			// We're inside or at the end of refToken, which is an ID.
			// Therefore, we're writing it!
			// Go back a token for our intent reference.
			// writingToken = refToken;
			refToken = backwardsTokenLink.get(refToken);
			if (refToken == null)
				return null;
		}
		HashSet<HCMIntent> intentSet = intentsOnNextToken.get(refToken);
		if (intentSet == null)
			return null;
		// optimizations / return null for no intents
		int sz = intentSet.size();
		if (sz == 0)
			return null;
		else if (intentSet.size() == 1)
			return intentSet.iterator().next().retrieve(spu, this);
		// alright, accumulate
		HashMap<String, HoverData> accumulated = new HashMap<>();
		for (HCMIntent hi : intentSet)
			accumulated.putAll(hi.retrieve(spu, this));
		return accumulated;
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
