/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;
import java.util.LinkedList;

import rals.expr.RALDiscard;
import rals.expr.RALExpr;
import rals.expr.RALStringVar;
import rals.expr.RALVAVar;
import rals.types.RALType;

/**
 * Contains in-scope variables, and also frees them when closed.
 * Good with try-with-resources pattern.
 * Note that these don't have to be specifically nested.
 */
public class ScopeContext {
	public final ScriptContext script;

	/**
	 * Scoped variables (including inherited)
	 * Note that the entries here will change chronologically as we move forward lexically in the blocks and as things get shadowed.
	 * That's normal.
	 */
	public HashMap<String, RALExpr> scopedVariables = new HashMap<>();

	public ScopeContext(ScriptContext parent) {
		script = parent;
		scopedVariables.put("ownr", new RALStringVar("ownr", parent.ownrType, true));
		// Dynamic VMVars would be nice, but we need hard logic anyway for, say, ownrType
		scopedVariables.put("from", new RALStringVar("from", parent.typeSystem.gAny, true));
		scopedVariables.put("_it_", new RALStringVar("_it_", parent.typeSystem.gAgentNullable, true));
		scopedVariables.put("part", new RALStringVar("part", parent.typeSystem.gInteger, false));
		scopedVariables.put("_p1_", new RALStringVar("_p1_", parent.typeSystem.gAny, true));
		scopedVariables.put("_p2_", new RALStringVar("_p2_", parent.typeSystem.gAny, true));
		scopedVariables.put("null", new RALStringVar("null", parent.typeSystem.gNull, true));
		scopedVariables.put("_", new RALDiscard(parent.typeSystem));
	}

	public ScopeContext(ScopeContext parent) {
		script = parent.script;
		scopedVariables.putAll(parent.scopedVariables);
	}

	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(int va) {
		String res = Integer.toString(va);
		if (res.length() == 1)
			return "va0" + res;
		return "va" + res;
	}

	/**
	 * Note: You still have to actualize the VA handle for this!
	 */
	public RALVAVar newLocal(String name, RALType type) {
		RALVAVar rvv = new RALVAVar(new IVAHandle() {
			@Override
			public String toString() {
				return name;
			}
		}, type);
		scopedVariables.put(name, rvv);
		return rvv;
	}
}
