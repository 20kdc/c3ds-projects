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
import rals.lex.DefInfo;
import rals.lex.Token;

/**
 * Storage of HCM data (since HCM data on all active LSP files remains in memory at all times)
 */
public class HCMStorage {
	// These *would* all be final, but it's getting confusing.
	public SrcPosMap<HCMScopeSnapshot> snapshots;
	public SrcPosMap<Token> lastTokenMap;
	public HashMap<Token, HoverData> hoverTokenOverrides;
	public HashMap<Token, Token> backwardsTokenLink;
	public HashMap<Token.ID, HCMIntent> hoverIntents;
	public HashMap<Token, HashSet<HCMIntent>> intentsOnNextToken;
	public HashMap<String, HoverData> allNamedTypes;
	public HashMap<String, HoverData> allConstants;
	public HashMap<String, HoverData> allCallables;
	public HashMap<Integer, HashMap<String, HoverData>> allCallablesAV;
	public HashMap<HCMRelativeIntent.Anchor, HCMRelativeIntent.Tracking> relativeIntentExprs;

	public HCMStorage() {
	}

	/**
	 * Gets hover information at a given point (or null for none)
	 */
	public HoverData getHoverData(SrcPosUntranslated tp) {
		Token tkn = lastTokenMap.get(tp);
		HoverData override = tkn != null ? hoverTokenOverrides.get(tkn) : null;
		if (tkn == null) {
			return null;
		} else if (override != null) {
			return override;
		} else if (tkn instanceof Token.ID) {
			Token.ID id = (Token.ID) tkn;
			HCMIntent hi = hoverIntents.get(id);
			if (hi != null) {
				Token former = backwardsTokenLink.get(tkn);
				return hi.retrieve(former, tp, this).get(id.text);
			}
			return null;
		} else {
			return null;
		}
	}

	private Token completionRefToken(SrcPosUntranslated spu) {
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
		if ((refToken instanceof Token.Kw) && (spu.lcLong == refToken.extent.start.lcLong)) {
			// We're at the start of a keyword. This probably means we're in a case like this:
			// attr(<HERE>);
			// Note that we are still quite capable of running into an ID *after* that,
			// if we're in the case of:
			// attr(SOMEID<HERE>);
			// In this case, we go back to SOMEID, then we need to go back a token again for our intent ref.
			refToken = backwardsTokenLink.get(refToken);
		}
		if ((refToken instanceof Token.ID) && (spu.lcLong <= refToken.extent.end.lcLong)) {
			// We're inside or at the end of refToken, which is an ID.
			// Therefore, we're writing it!
			// Go back a token for our intent reference.
			// writingToken = refToken;
			refToken = backwardsTokenLink.get(refToken);
		}
		return refToken;
	}

	/**
	 * Debug information from hover
	 */
	public String hoverDebugPrefix(SrcPosUntranslated spu) {
		StringBuilder sb = new StringBuilder();
		sb.append("lT:");
		Token lt = lastTokenMap.get(spu);
		sb.append(lt);
		if (lt != null) {
			sb.append(" hI:");
			sb.append(hoverIntents.get(lt));
		}
		Token refToken = completionRefToken(spu);
		sb.append(" cT:");
		sb.append(refToken);
		if (refToken != null) {
			sb.append(" cI");
			HashSet<HCMIntent> intentSet = intentsOnNextToken.get(refToken);
			if (intentSet != null) {
				for (HCMIntent hi : intentSet) {
					sb.append(":");
					sb.append(hi);
				}
				sb.append(";");
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * Gets completion information at a given point (or null for none)
	 */
	public Map<String, HoverData> getCompletion(SrcPosUntranslated spu) {
		Token refToken = completionRefToken(spu);
		if (refToken == null)
			return null;
		HashSet<HCMIntent> intentSet = intentsOnNextToken.get(refToken);
		if (intentSet == null)
			return null;
		// optimizations / return null for no intents
		int sz = intentSet.size();
		if (sz == 0)
			return null;
		else if (intentSet.size() == 1)
			return intentSet.iterator().next().retrieve(refToken, spu, this);
		// alright, accumulate
		HashMap<String, HoverData> accumulated = new HashMap<>();
		for (HCMIntent hi : intentSet)
			accumulated.putAll(hi.retrieve(refToken, spu, this));
		return accumulated;
	}

	/**
	 * Hover data.
	 */
	public static final class HoverData {
		public final String text;
		public final DefInfo defInfo;
		public HoverData(String t, DefInfo di) {
			text = t;
			defInfo = di;
		}
	}
}
