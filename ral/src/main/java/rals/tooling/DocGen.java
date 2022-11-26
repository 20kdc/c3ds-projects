/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import rals.code.Macro;
import rals.code.MacroDefSet;
import rals.expr.RALCallable;
import rals.expr.RALConstant;
import rals.hcm.HCMHoverDataGenerators;
import rals.lex.DefInfo;
import rals.lex.DefInfo.At;
import rals.parser.IncludeParseContext;
import rals.types.RALType;

/**
 * Documentation generator!
 */
public class DocGen {
	public static void build(StringBuilder sb, IncludeParseContext ic, Rule[] r) {
		sb.append("### Macros\n");
		sb.append("\n");
		LinkedList<String> keys = new LinkedList<>(ic.module.callable.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			RALCallable rc = ic.module.callable.get(s);
			buildCallable(sb, rc, r);
		}
		sb.append("### Constants\n");
		sb.append("\n");
		keys = new LinkedList<>(ic.typeSystem.namedConstants.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			RALConstant rc = ic.typeSystem.namedConstants.get(s);
			DefInfo.At di = ic.typeSystem.namedConstantsDefPoints.get(s);
			if (di != null) {
				String officialDefLoc = translateDefInfo(di);
				if (matchesRules(officialDefLoc, r)) {
					sb.append("#### `");
					sb.append(rc.slot(0).type);
					sb.append(" ");
					sb.append(s);
					sb.append(" = ");
					sb.append(rc);
					sb.append("`\n\n");
					showDocBody(sb, di);
				}
			}
		}
		sb.append("### Types\n");
		sb.append("\n");
		keys.clear();
		for (Map.Entry<String, RALType> rt : ic.typeSystem.getAllNamedTypes())
			keys.add(rt.getKey());
		Collections.sort(keys);
		for (String s : keys) {
			RALType rt = ic.typeSystem.byName(s);
			DefInfo di = ic.typeSystem.getNamedTypeDefInfo(s);
			if (di instanceof DefInfo.At) {
				String officialDefLoc = translateDefInfo((DefInfo.At) di);
				if (matchesRules(officialDefLoc, r)) {
					sb.append("#### `");
					sb.append(s);
					sb.append(": ");
					sb.append(rt.getFullDescription());
					sb.append("`\n\n");
					showDocBody(sb, (DefInfo.At) di);
				}
			}
		}
	}
	private static String translateDefInfo(At at) {
		return at.srcRange.file.shortName;
	}
	private static void buildCallable(StringBuilder sb, RALCallable rc, Rule[] r) {
		if (rc instanceof MacroDefSet) {
			HashMap<Integer, RALCallable> rcx = ((MacroDefSet) rc).map;
			LinkedList<Integer> l = new LinkedList<>(rcx.keySet());
			Collections.sort(l);
			for (Integer i : l)
				buildCallable(sb, rcx.get(i), r);
		} else if (rc instanceof Macro) {
			Macro mac = (Macro) rc;
			String officialDefLoc = translateDefInfo(mac.defInfo);
			if (matchesRules(officialDefLoc, r)) {
				sb.append("#### `");
				sb.append(mac.name);
				HCMHoverDataGenerators.showMacroArgs(sb, mac.args);
				if (mac.precompiledCode != null) {
					sb.append(": ");
					HCMHoverDataGenerators.showSlots(sb, mac.precompiledCode.slots());
				}
				sb.append("`\n\n");
				showDocBody(sb, mac.defInfo);
			}
		}
	}
	private static void showDocBody(StringBuilder sb, DefInfo.At di) {
		if (di.docComment != null) {
			sb.append(di.docComment);
			sb.append("\n\n");
		}
		sb.append("_Declared in:_ `");
		sb.append(translateDefInfo(di));
		sb.append("`\n\n");
	}
	public static boolean matchesRules(String spf, Rule[] r) {
		boolean matches = true;
		for (Rule rule : r)
			if (spf.startsWith(rule.prefix))
				matches = rule.add;
		return matches;
	}
	public static class Rule {
		public final boolean add;
		public final String prefix;
		public Rule(String pfx) {
			if (pfx.startsWith("+")) {
				add = true;
			} else if (pfx.startsWith("-")) {
				add = false;
			} else {
				throw new RuntimeException("invalid rule prefix on " + pfx);
			}
			prefix = pfx.substring(1);
		}
	}
}
