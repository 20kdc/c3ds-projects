/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;
import java.util.Map;

import rals.diag.DiagRecorder;
import rals.expr.RALCallable;
import rals.expr.RALConstant;
import rals.hcm.IHCMRecorder;
import rals.stmt.*;
import rals.types.*;

/**
 * Represents the content of the output .cos file.
 * If you took away the Module, the output .cos file would be empty.
 * If you took away the TypeSystem, nothing would make sense (and things like constants would be missing).
 */
public class ScriptsUR {
	public final TypeSystem typeSystem;

	public RALBlock installScript;
	public HashMap<ScriptIdentifier, RALStatementUR> eventScripts = new HashMap<>();
	public RALBlock removeScript;

	public HashMap<String, MacroDefSet> macroDefs = new HashMap<>();

	public ScriptsUR(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
	}

	public void addMacro(String name, int count, RALCallable.Global c) {
		if (!typeSystem.namedConstants.containsKey(name)) {
			MacroDefSet mds = macroDefs.computeIfAbsent(name, (n) -> new MacroDefSet(name));
			typeSystem.declareConst(name, c.getDefInfo(), new RALConstant.Callable(typeSystem.gLambdaAny, mds));
		}
		MacroDefSet res = macroDefs.get(name);
		if (res == null)
			throw new RuntimeException(name + " can't have a macro declared as a different type of value is already present.");
		res.addMacro(count, c);
	}

	public Scripts resolve(DiagRecorder diags, IHCMRecorder hcm) {
		UnresolvedWorld world = new UnresolvedWorld(typeSystem, this, diags, hcm);
		Scripts scripts = new Scripts(typeSystem, diags);

		// This ensures macros are ready
		for (RALCallable.Global rc : macroDefs.values())
			rc.precompile(world);

		if (installScript != null) {
			ScriptContext installScriptCtx = new ScriptContext(world, typeSystem.gAgentNullable, typeSystem.gAny, typeSystem.gAny, typeSystem.gAny);
			scripts.installScript = installScriptCtx.resolveStmt(installScript);
		}

		for (Map.Entry<ScriptIdentifier, RALStatementUR> eventScript : eventScripts.entrySet()) {
			ScriptIdentifier k = eventScript.getKey();
			RALStatementUR v = eventScript.getValue();
			RALType oOwnr = typeSystem.byClassifier(k.classifier);
			RALType oFrom = typeSystem.gAny;
			RALType oP1 = typeSystem.gAny;
			RALType oP2 = typeSystem.gAny;
			RALType override = typeSystem.overrideOwnr.get(k.script);
			if (override != null) {
				oOwnr = override;
				oFrom = oOwnr;
			}
			ScriptContext scr = new ScriptContext(world, oOwnr, oFrom, oP1, oP2);
			scripts.eventScripts.put(k, scr.resolveStmt(v));
		}

		if (removeScript != null) {
			ScriptContext removeScriptCtx = new ScriptContext(world, typeSystem.gNull, typeSystem.gAny, typeSystem.gAny, typeSystem.gAny);
			scripts.removeScript = removeScriptCtx.resolveStmt(removeScript);
		}

		return scripts;
	}

	public void addInstall(RALStatementUR parseStatement) {
		if (installScript == null)
			installScript = new RALBlock(parseStatement.lineNumber, false);
		installScript.content.add(parseStatement);
	}

	public void addRemove(RALStatementUR parseStatement) {
		if (removeScript == null)
			removeScript = new RALBlock(parseStatement.lineNumber, false);
		removeScript.content.add(parseStatement);
	}
}
