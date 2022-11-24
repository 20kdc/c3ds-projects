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
import java.util.Map;

import rals.code.ScopeContext;
import rals.diag.SrcRange;
import rals.expr.RALConstant;
import rals.hcm.HCMStorage.HoverData;
import rals.lex.Token;
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
	public final HashSet<Token.ID> idReferences = new HashSet<>();
	public final HashSet<Token.ID> typeNameReferences = new HashSet<>();

	public ActualHCMRecorder(IDocPath docPath) {
		targetDocPath = docPath;
	}

	@Override
	public void readToken(Token tkn) {
		if (tkn.isInDP(targetDocPath)) {
			lastTokenMap.putUntilEnd(tkn.extent.start, tkn);
		}
	}

	@Override
	public void idReference(Token.ID tkn) {
		if (tkn.isInDP(targetDocPath))
			idReferences.add(tkn);
	}

	@Override
	public void namedTypeReference(Token.ID tkn) {
		if (tkn.isInDP(targetDocPath))
			typeNameReferences.add(tkn);
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
		for (Map.Entry<String, RALType> nt : info.typeSystem.namedTypes.entrySet())
			allNamedTypes.put(nt.getKey(), HCMHoverDataGenerators.typeHoverData(nt.getKey(), nt.getValue()));

		HashMap<String, HoverData> allNamedConstants = new HashMap<>();
		for (Map.Entry<String, RALConstant> nt : info.typeSystem.namedConstants.entrySet()) {
			allNamedConstants.put(nt.getKey(), HCMHoverDataGenerators.constHoverData(nt.getKey(), nt.getValue()));
		}
		return new HCMStorage(snapshotsSPM, lastTokenMap, idReferences, typeNameReferences, allNamedTypes, allNamedConstants);
	}
}
