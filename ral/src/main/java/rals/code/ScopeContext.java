/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;

import rals.expr.*;
import rals.types.*;

/**
 * Contains in-scope variables, and also frees them when closed.
 * Good with try-with-resources pattern.
 * Note that these don't have to be specifically nested.
 */
public class ScopeContext {
	public final ScriptContext script;
	public final UnresolvedWorld world;

	/**
	 * Scoped variables (including inherited)
	 * Note that the entries here will change chronologically as we move forward lexically in the blocks and as things get shadowed.
	 * That's normal.
	 */
	public HashMap<String, RALExprSlice> scopedVariables = new HashMap<>();

	public ScopeContext(ScriptContext parent) {
		script = parent;
		world = script.world;
		scopedVariables.put("ownr", new RALVarSI(RALSpecialInline.Ownr, parent.ownrType, false));
		// Dynamic VMVars would be nice, but we need hard logic anyway for, say, ownrType
		scopedVariables.put("from", new RALVarString.Fixed("from", parent.fromType, false));
		scopedVariables.put("_it_", new RALVarString.Fixed("_it_", world.types.gAgentNullable, false));
		scopedVariables.put("part", new RALVarPart(world.types.gInteger));
		scopedVariables.put("_p1_", new RALVarString.Fixed("_p1_", parent.p1Type, false));
		scopedVariables.put("_p2_", new RALVarString.Fixed("_p2_", parent.p2Type, false));
		scopedVariables.put("null", new RALVarString.Fixed("null", world.types.gNull, false));
		scopedVariables.put("targ", new RALVarTarg(world.types.gAgentNullable));
		scopedVariables.put("_", new RALDiscard(world.types, 1));
	}

	public ScopeContext(ScopeContext parent) {
		script = parent.script;
		world = script.world;
		scopedVariables.putAll(parent.scopedVariables);
	}

	/**
	 * Note: You still have to actualize the VA handle for this!
	 */
	public RALVarVA newLocal(String name, RALType type) {
		RALVarVA rvv = new RALVarVA(new IVAHandle() {
			@Override
			public String toString() {
				return name;
			}
		}, type);
		scopedVariables.put(name, rvv);
		return rvv;
	}
}
