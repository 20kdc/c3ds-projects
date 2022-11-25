/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.HashMap;
import java.util.Map;

import rals.code.ScopeContext;
import rals.diag.SrcPos;
import rals.types.RALType;

/**
 * Snapshot of variables in scope at a specific point.
 * Also includes classifiers.
 */
public class HCMScopeSnapshot {
	public final SrcPos takenAt;
	public final HashMap<String, HCMStorage.HoverData> contents = new HashMap<>();

	public HCMScopeSnapshot(SrcPos at, ScopeContext sc) {
		takenAt = at;
		for (Map.Entry<String, RALType> var : sc.world.types.getAllNamedTypes()) {
			String k = var.getKey();
			RALType rt = var.getValue();
			if (rt instanceof RALType.AgentClassifier)
				contents.put(k, HCMHoverDataGenerators.typeHoverData(k, rt, sc.world.types.getNamedTypeDefInfo(k)));
		}
		for (ScopeContext.LVar var : sc.scopedVariables.values())
			contents.put(var.name, HCMHoverDataGenerators.varHoverData(var));
	}
}
