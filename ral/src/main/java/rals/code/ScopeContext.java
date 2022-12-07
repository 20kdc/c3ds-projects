/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;

import rals.diag.SrcPos;
import rals.expr.*;
import rals.lex.DefInfo;
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
	public HashMap<String, LVar> scopedVariables = new HashMap<>();

	public ScopeContext(ScriptContext parent) {
		script = parent;
		world = script.world;
		DefInfo dOwn = new DefInfo.Builtin("ownr is the agent in which this script runs.");
		setLoc("ownr", dOwn, new RALVarSI(RALSpecialInline.Ownr, parent.ownrType, false));
		// Dynamic VMVars would be nice, but we need hard logic anyway for, say, ownrType
		DefInfo dFrm = new DefInfo.Builtin("from is usually the 'cause' of this script running.");
		setLoc("from", dFrm, new RALVarString.Fixed("from", parent.fromType, false));
		DefInfo dItt = new DefInfo.Builtin("For Creature agents, _it_ is the current object of attention at the start of this script.");
		setLoc("_it_", dItt, new RALVarString.Fixed("_it_", world.types.gAgentNullable, false));
		DefInfo dPm1 = new DefInfo.Builtin("_p1_ is a parameter. The engine can set it for some scripts, or it can be controlled by the sender of a message.");
		setLoc("_p1_", dPm1, new RALVarString.Fixed("_p1_", parent.p1Type, true));
		DefInfo dPm2 = new DefInfo.Builtin("_p2_ is a parameter. The engine can set it for some scripts, or it can be controlled by the sender of a message.");
		setLoc("_p2_", dPm2, new RALVarString.Fixed("_p2_", parent.p2Type, true));
		DefInfo dNul = new DefInfo.Builtin("null is an agent reference that does not actually reference an agent.");
		setLoc("null", dNul, new RALVarString.Fixed("null", world.types.gNull, false));
		DefInfo dDsc = new DefInfo.Builtin("_, or discard, is a fake variable which tells RAL to dispose of a value.");
		setLoc("_",    dDsc, new RALDiscard(world.types, 1));
		regenerateTarg(null, world.types.gAgentNullable);
	}

	public ScopeContext(ScopeContext parent) {
		script = parent.script;
		world = script.world;
		scopedVariables.putAll(parent.scopedVariables);
	}

	public void regenerateTarg(SrcPos at, RALType type) {
		String doc = "targ is an agent reference that acts as an implicit parameter to many CAOS commands.";
		DefInfo dTrg = (at != null) ? new DefInfo.At(at, doc) : new DefInfo.Builtin(doc);
		setLoc("targ", dTrg, new RALVarTarg(type));
	}

	public void setLoc(String name, DefInfo def, RALExprSlice slice) {
		scopedVariables.put(name, new LVar(name, def, slice));
	}

	/**
	 * Note: You still have to actualize the VA handle for this!
	 */
	public RALVarVA newLocal(String name, DefInfo def, RALType type) {
		RALVarVA rvv = new RALVarVA(new IVAHandle() {
			@Override
			public String toString() {
				return name;
			}
		}, type);
		setLoc(name, def, rvv);
		return rvv;
	}

	/**
	 * Local variable.
	 * Note this is immutable - this is in case we want to capture these in scope snapshots.
	 */
	public static class LVar {
		/**
		 * Must always be equal to the key used to access the variable.
		 */
		public final String name;
		/**
		 * Nullable, but try not to abuse it.
		 */
		public final DefInfo definition;
		public final RALExprSlice content;
		public LVar(String n, DefInfo def, RALExprSlice c) {
			name = n;
			definition = def;
			content = c;
		}
	}
}
