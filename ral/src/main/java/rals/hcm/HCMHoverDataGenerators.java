/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import rals.code.Macro;
import rals.code.MacroArg;
import rals.code.MacroDefSet;
import rals.expr.RALCallable;
import rals.expr.RALConstant;
import rals.expr.RALSlot;
import rals.hcm.HCMStorage.HoverData;
import rals.types.RALType;

/**
 * Generators of HCM hover data.
 */
public class HCMHoverDataGenerators {
	public static void showSlot(StringBuilder sb, RALSlot slot) {
		sb.append(slot.type);
		sb.append("/");
		sb.append(slot.perms);
	}
	public static void showSlots(StringBuilder sb, RALSlot[] slots) {
		if (slots.length != 1) {
			sb.append("(");
			boolean first = true;
			for (RALSlot rs : slots) {
				if (!first)
					sb.append(", ");
				first = false;
				showSlot(sb, rs);
			}
			sb.append(")");
		} else {
			showSlot(sb, slots[0]);
		}
	}
	public static HCMStorage.HoverData varHoverData(String name, RALSlot[] slots) {
		StringBuilder sb = new StringBuilder();
		showSlots(sb, slots);
		sb.append(" ");
		sb.append(name);
		return new HCMStorage.HoverData(sb.toString());
	}
	public static HCMStorage.HoverData constHoverData(String name, RALConstant c) {
		StringBuilder sb = new StringBuilder();
		showSlots(sb, c.slots());
		sb.append(" ");
		sb.append(name);
		sb.append(" = ");
		sb.append(c.toString());
		return new HCMStorage.HoverData(sb.toString());
	}
	public static HoverData typeHoverData(String k, RALType rt) {
		return new HCMStorage.HoverData(k + ": " + rt.getFullDescription());
	}
	private static void showCallable(StringBuilder sb, String k, RALCallable rc) {
		if (rc instanceof MacroDefSet) {
			MacroDefSet mds = (MacroDefSet) rc;
			LinkedList<Integer> keys = new LinkedList<>(mds.map.keySet());
			Collections.sort(keys);
			for (Integer ent : keys)
				showCallable(sb, k, mds.map.get(ent));
		} else if (rc instanceof Macro) {
			showSlots(sb, ((Macro) rc).precompiledCode.slots());
			sb.append(" ");
			sb.append(k);
			sb.append("(");
			boolean first = true;
			for (MacroArg ma : ((Macro) rc).args) {
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(ma.type);
				sb.append(" ");
				if (ma.isInline)
					sb.append("&");
				sb.append(ma.name);
			}
			sb.append(")\n");
		} else {
			sb.append(k);
			sb.append(": ");
			sb.append(rc);
			sb.append("\n");
		}
	}
	public static HoverData callableHoverData(String k, RALCallable rc) {
		StringBuilder sb = new StringBuilder();
		showCallable(sb, k, rc);
		return new HCMStorage.HoverData(sb.toString());
	}
}
