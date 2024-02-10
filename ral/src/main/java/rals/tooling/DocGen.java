/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import rals.code.Macro;
import rals.code.MacroDefSet;
import rals.expr.RALCallable;
import rals.expr.RALConstant;
import rals.hcm.HCMHoverDataGenerators;
import rals.lex.DefInfo;
import rals.parser.IncludeParseContext;
import rals.types.AgentInterface;
import rals.types.AgentInterface.Attachment;
import rals.types.RALType;

/**
 * Documentation generator!
 */
public class DocGen {
	public static void build(String baseIndent, StringBuilder sb, IncludeParseContext ic, Rule[] r) {
		sb.append(baseIndent + " Macros\n");
		sb.append("\n");
		LinkedList<String> keys = new LinkedList<>(ic.typeSystem.namedConstants.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			RALConstant rc = ic.typeSystem.namedConstants.get(s);
			if (rc instanceof RALConstant.Callable)
				buildCallable(baseIndent, sb, ((RALConstant.Callable) rc).value, r);
		}
		sb.append(baseIndent + " Constants\n");
		sb.append("\n");
		for (String s : keys) {
			RALConstant rc = ic.typeSystem.namedConstants.get(s);
			if (rc instanceof RALConstant.Callable)
				continue;
			DefInfo di = ic.typeSystem.namedConstantsDefPoints.get(s);
			if (di != null) {
				if (matchesRules(di, r)) {
					sb.append(baseIndent + "# `");
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
		sb.append(baseIndent + " Types\n");
		sb.append("\n");
		keys.clear();
		for (Map.Entry<String, RALType> rt : ic.typeSystem.getAllNamedTypes())
			keys.add(rt.getKey());
		Collections.sort(keys);
		for (String s : keys) {
			RALType rt = ic.typeSystem.byName(s);
			DefInfo di = ic.typeSystem.getNamedTypeDefInfo(s);
			if (matchesRules(di, r)) {
				sb.append(baseIndent + "# `");
				sb.append(s);
				sb.append(": ");
				sb.append(rt.getFullDescription());
				sb.append("`\n\n");
				showDocBody(sb, di);
				// Now go into agent interfaces
				LinkedList<Attachment> fields = new LinkedList<>();
				LinkedList<Attachment> sm = new LinkedList<>();
				for (AgentInterface ai : rt.getInterfaces()) {
					if (ai.canonicalType == rt) {
						for (Attachment a : ai.fields.values())
							fields.add(a);
						HashSet<Attachment> al = new HashSet<>(ai.messages.values());
						al.addAll(ai.scripts.values());
						for (Attachment a : al)
							sm.add(a);
					}
				}
				Collections.sort(fields);
				for (Attachment a : fields)
					doAttachment(baseIndent, sb, a, r);
				Collections.sort(sm);
				for (Attachment a : sm)
					doAttachment(baseIndent, sb, a, r);
			}
		}
	}
	private static void doAttachment(String baseIndent, StringBuilder sb, Attachment a, Rule[] r) {
		if (matchesRules(a.defInfo, r)) {
			sb.append(baseIndent + "## `");
			sb.append(a.toString());
			sb.append("`\n\n");
			showDocBody(sb, (DefInfo.At) a.defInfo);
		}
	}
	private static void buildCallable(String baseIndent, StringBuilder sb, RALCallable rc, Rule[] r) {
		if (rc instanceof MacroDefSet) {
			HashMap<Integer, RALCallable.Global> rcx = ((MacroDefSet) rc).map;
			LinkedList<Integer> l = new LinkedList<>(rcx.keySet());
			Collections.sort(l);
			for (Integer i : l)
				buildCallable(baseIndent, sb, rcx.get(i), r);
		} else if (rc instanceof Macro) {
			Macro mac = (Macro) rc;
			if (matchesRules(mac.defInfo, r)) {
				sb.append(baseIndent + "# `");
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
	private static void showDocBody(StringBuilder sb, DefInfo di) {
		if (di.docComment != null) {
			sb.append(di.docComment);
			sb.append("\n\n");
		}
		sb.append("_Declared in:_ `");
		sb.append(translateDefInfo(di));
		sb.append("`\n\n");
	}
	/**
	 * Translates a DefInfo to user-visible text.
	 */
	private static String translateDefInfo(DefInfo di) {
		if (di instanceof DefInfo.Builtin)
			return "Built-in";
		if (di instanceof DefInfo.At)
			return di.srcRange.file.shortName;
		return "Unknown DefInfo type: " + di;
	}
	/**
	 * Matches a DefInfo against the set of rules, including the fancy specifics for particular DefInfo kinds.
	 */
	private static boolean matchesRules(DefInfo di, Rule[] r) {
		boolean matches = true;
		if (di instanceof DefInfo.Builtin) {
			for (Rule rule : r)
				if (rule.prefix.equals("BUILTIN"))
					matches = rule.add;
		}
		if (di instanceof DefInfo.At) {
			String spf = di.srcRange.file.shortName;
			for (Rule rule : r) {
				if (rule.prefix.equals("BUILTIN"))
					continue;
				if (spf.startsWith(rule.prefix))
					matches = rule.add;
			}
		}
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
