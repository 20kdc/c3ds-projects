/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import rals.code.MacroDefSet;
import rals.code.ScopeContext;
import rals.diag.SrcPosFile;
import rals.diag.SrcRange;
import rals.expr.RALCallable;
import rals.expr.RALConstant;
import rals.expr.RALExprSlice;
import rals.expr.RALExprUR;
import rals.hcm.HCMRelativeIntent.Anchor;
import rals.hcm.HCMStorage.HoverData;
import rals.lex.Token;
import rals.lex.Token.ID;
import rals.parser.IDocPath;
import rals.parser.IncludeParseContext;
import rals.types.RALType;

/**
 * HCM recorder in cases where HCM recording is wanted.
 */
public class ActualHCMRecorder implements IHCMRecorder {
	public final IDocPath targetDocPath;
	// This is stored as a HashMap and then translated later.
	// This helps to prevent unwanted overwrites.
	public final HashMap<Long, HCMScopeSnapshot> snapshots = new HashMap<>();
	public final SrcPosMap<Token> lastTokenMap = new SrcPosMap<>();
	public final HashMap<Token, HoverData> hoverTokenOverrides = new HashMap<>();
	public final HashMap<Token, Token> backwardsTokenLink = new HashMap<>();
	public final HashMap<Token.ID, HCMIntent> hoverIntents = new HashMap<>();
	public final HashMap<Token, HashSet<HCMIntent>> intentsOnNextToken = new HashMap<>();
	public final HashMap<HCMRelativeIntent.Anchor, HCMRelativeIntent.Tracking> relativeIntentExprs = new HashMap<>();
	public final HashMap<RALExprUR, LinkedList<HCMRelativeIntent.Tracking>> relativeIntentRoutingTable = new HashMap<>();
	public Token currentRequestedToken;
	public Token lastReadToken;
	public HCMIntent autoHoverHolding;
	public RALExprUR[] autoHoverHoldingParams;

	public ActualHCMRecorder(IDocPath docPath) {
		targetDocPath = docPath;
	}

	@Override
	public void readToken(Token tkn) {
		if (!tkn.isInDP(targetDocPath))
			return;
		// setup backwards token chain
		// (why isn't this in Token? sort of a mixed bag of reasons. don't want to encourage Parser to use it, for one...)
		if (lastReadToken != null)
			backwardsTokenLink.put(tkn, lastReadToken);
		lastReadToken = tkn;
		// continue
		lastTokenMap.putUntilEnd(tkn.extent.start, tkn);
	}

	@Override
	public void parserRequestedToken(Token tkn, boolean actualRequest) {
		currentRequestedToken = tkn;
		if (actualRequest) {
			HCMIntent heldAH = autoHoverHolding;
			RALExprUR[] heldAP = autoHoverHoldingParams;
			autoHoverHolding = null;
			autoHoverHoldingParams = null;
			if (!tkn.isInDP(targetDocPath))
				return;
			if (heldAH != null)
				if (tkn instanceof ID)
					setTokenHoverRelIntent((ID) tkn, heldAH, heldAP);
		}
	}

	@Override
	public void addCompletionRelIntentToNextToken(HCMIntent intent, boolean autoHover, RALExprUR... params) {
		// always do this, just in case.
		if (autoHover) {
			autoHoverHolding = intent;
			autoHoverHoldingParams = params;
		}
		// continue.
		if (currentRequestedToken == null)
			return;
		if (!currentRequestedToken.isInDP(targetDocPath))
			return;
		// main
		HashSet<HCMIntent> hs = intentsOnNextToken.get(currentRequestedToken);
		if (hs == null) {
			hs = new HashSet<>();
			intentsOnNextToken.put(currentRequestedToken, hs);
		}
		hs.add(intent);
		// params
		if (params != null)
			createRelIntentLink((HCMRelativeIntent) intent, currentRequestedToken, params);
	}

	@Override
	public void setTokenHoverRelIntent(ID tkn, HCMIntent intent, RALExprUR... params) {
		if (!tkn.isInDP(targetDocPath))
			return;
		hoverIntents.put(tkn, intent);
		if (params != null) {
			Token prev = backwardsTokenLink.get(tkn);
			if (prev != null)
				createRelIntentLink((HCMRelativeIntent) intent, prev, params);
		}
	}

	/**
	 * Links relative intents and their parameters to track them during compilation.
	 */
	private void createRelIntentLink(HCMRelativeIntent intent, Token prev, RALExprUR[] params) {
		HCMRelativeIntent.Tracking trk = new HCMRelativeIntent.Tracking(params);
		relativeIntentExprs.put(new Anchor(intent, prev), trk);
		for (RALExprUR p : params) {
			LinkedList<HCMRelativeIntent.Tracking> ll = relativeIntentRoutingTable.get(p);
			if (ll == null) {
				ll = new LinkedList<>();
				relativeIntentRoutingTable.put(p, ll);
			}
			ll.add(trk);
		}
	}

	@Override
	public void assignIncludeRange(Token first, Token last, SrcPosFile spf) {
		if (!first.isInDP(targetDocPath))
			return;
		HoverData hd = HCMHoverDataGenerators.includeHoverData(spf);
		hoverTokenOverrides.put(last, hd);
		while (last != first) {
			last = backwardsTokenLink.get(last);
			if (last == null)
				throw new RuntimeException("Ran off of the start during assignIncludeRange");
			hoverTokenOverrides.put(last, hd);
		}
	}

	@Override
	public void resolvePre(SrcRange rs, ScopeContext scope) {
		if (rs.isInDP(targetDocPath))
			snapshots.put(rs.start.lcLong, new HCMScopeSnapshot(rs.start, scope));
	}

	@Override
	public void resolvePost(SrcRange rs, ScopeContext scope) {
		if (rs.isInDP(targetDocPath))
			snapshots.put(rs.end.lcLong, new HCMScopeSnapshot(rs.end, scope));
	}

	@Override
	public void onResolveExpression(RALExprUR src, RALExprSlice dst) {
		// Resolve relative intent contents (this contributes, i.e. macro arguments to hover data)
		LinkedList<HCMRelativeIntent.Tracking> trkL = relativeIntentRoutingTable.get(src);
		if (trkL != null)
			for (HCMRelativeIntent.Tracking trk : trkL)
				trk.contribute(src, dst);
	}

	public HCMStorage compile(IncludeParseContext info) {
		ArrayList<HCMScopeSnapshot> snapshotsList = new ArrayList<>(snapshots.values());
		snapshotsList.sort(new Comparator<HCMScopeSnapshot>() {
			@Override
			public int compare(HCMScopeSnapshot o1, HCMScopeSnapshot o2) {
				if (o1.takenAt.lcLong < o2.takenAt.lcLong)
					return -1;
				else if (o1.takenAt.lcLong > o2.takenAt.lcLong)
					return 1;
				return 0;
			}
		});
		SrcPosMap<HCMScopeSnapshot> snapshotsSPM = new SrcPosMap<>();
		for (HCMScopeSnapshot hss : snapshotsList)
			snapshotsSPM.putUntilEnd(hss.takenAt, hss);

		HashMap<String, HoverData> allNamedTypes = new HashMap<>();
		for (Map.Entry<String, RALType> nt : info.typeSystem.getAllNamedTypes())
			allNamedTypes.put(nt.getKey(), HCMHoverDataGenerators.typeHoverData(nt.getKey(), nt.getValue(), info.typeSystem.getNamedTypeDefInfo(nt.getKey())));

		HashMap<String, HoverData> allConstants = new HashMap<>();
		for (Map.Entry<String, RALConstant> nt : info.typeSystem.namedConstants.entrySet()) {
			allConstants.put(nt.getKey(), HCMHoverDataGenerators.constHoverData(nt.getKey(), nt.getValue(), info.typeSystem.namedConstantsDefPoints.get(nt.getKey())));
		}

		HashMap<String, HoverData> allCallables = new HashMap<>();
		for (Map.Entry<String, RALCallable> nt : info.module.callable.entrySet()) {
			allCallables.put(nt.getKey(), HCMHoverDataGenerators.callableHoverData(nt.getKey(), nt.getValue()));
		}

		HashMap<Integer, HashMap<String, HoverData>> allCallablesAV = new HashMap<>();
		for (Map.Entry<String, MacroDefSet> nt : info.module.macroDefs.entrySet()) {
			for (Map.Entry<Integer, RALCallable> nt2 : nt.getValue().map.entrySet()) {
				HashMap<String, HoverData> tgt = allCallablesAV.computeIfAbsent(nt2.getKey(), (k) -> new HashMap<String, HoverData>());
				tgt.put(nt.getKey(), HCMHoverDataGenerators.callableHoverData(nt.getKey(), nt2.getValue()));
			}
		}

		HCMStorage res = new HCMStorage();
		res.snapshots = snapshotsSPM;
		res.lastTokenMap = lastTokenMap;
		res.hoverTokenOverrides = hoverTokenOverrides;
		res.backwardsTokenLink = backwardsTokenLink;
		res.hoverIntents = hoverIntents;
		res.intentsOnNextToken = intentsOnNextToken;
		res.allNamedTypes = allNamedTypes;
		res.allConstants = allConstants;
		res.allCallables = allCallables;
		res.allCallablesAV = allCallablesAV;
		res.relativeIntentExprs = relativeIntentExprs;
		return res;
	}
}
