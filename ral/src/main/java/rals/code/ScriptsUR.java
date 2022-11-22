/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import rals.diag.DiagRecorder;
import rals.expr.RALCallable;
import rals.stmt.RALBlock;
import rals.stmt.RALInlineStatement;
import rals.stmt.RALStatement;
import rals.stmt.RALStatementUR;
import rals.types.RALType;
import rals.types.ScriptIdentifier;
import rals.types.TypeSystem;

/**
 * Represents the content of the output .cos file.
 * If you took away the Module, the output .cos file would be empty.
 * If you took away the TypeSystem, nothing would make sense (and things like constants would be missing).
 */
public class ScriptsUR {
	public RALBlock installScript;
	public HashMap<ScriptIdentifier, RALStatementUR> eventScripts = new HashMap<>();
	public RALBlock removeScript;

	public HashMap<String, MacroDefSet> macroDefs = new HashMap<>();
	public HashMap<String, RALCallable> callable = new HashMap<>();

	public ScriptsUR() {
		
	}

	public void addMacro(String name, int count, RALCallable c) {
		if (!callable.containsKey(name)) {
			MacroDefSet mds = macroDefs.computeIfAbsent(name, (n) -> new MacroDefSet(name));
			callable.put(name, mds);
		}
		MacroDefSet res = macroDefs.get(name);
		if (res == null)
			throw new RuntimeException(name + " can't have a macro declared as a different type of callable is already present.");
		res.addMacro(count, c);
	}

	public Scripts resolve(TypeSystem ts, DiagRecorder diags) {
		Scripts scripts = new Scripts();

		ScriptContext installScriptCtx = new ScriptContext(ts, this, diags, ts.gAny, ts.gAny, ts.gAny, ts.gAny);
		scripts.installScript = resolveStmt(installScriptCtx, installScript);

		for (Map.Entry<ScriptIdentifier, RALStatementUR> eventScript : eventScripts.entrySet()) {
			ScriptIdentifier k = eventScript.getKey();
			RALStatementUR v = eventScript.getValue();
			RALType oOwnr = ts.byClassifier(k.classifier);
			RALType oFrom = ts.gAny;
			RALType oP1 = ts.gAny;
			RALType oP2 = ts.gAny;
			RALType override = ts.overrideOwnr.get(k.script);
			if (override != null) {
				oOwnr = override;
				oFrom = oOwnr;
			}
			ScriptContext scr = new ScriptContext(ts, this, diags, oOwnr, oFrom, oP1, oP2);
			scripts.eventScripts.put(k, resolveStmt(scr, v));
		}

		ScriptContext removeScriptCtx = new ScriptContext(ts, this, diags, ts.gNull, ts.gAny, ts.gAny, ts.gAny);
		scripts.removeScript = resolveStmt(removeScriptCtx, removeScript);

		return scripts;
	}

	private RALStatement resolveStmt(ScriptContext scr, RALStatementUR v) {
		try {
			return v.resolve(new ScopeContext(scr));
		} catch (Exception ex) {
			scr.diags.error(v.lineNumber, "failed resolving: ", ex);
			return new RALInlineStatement.Resolved(v.lineNumber, new String[] {"STOP * RAL resolveStmt error"});
		}
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
