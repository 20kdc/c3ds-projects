/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;
import java.util.Map;

import rals.expr.RALCallable;
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
public class Module {
	public RALStatementUR installScript;
	public HashMap<ScriptIdentifier, RALStatementUR> eventScripts = new HashMap<>();
	public RALStatementUR removeScript;

	public HashMap<String, MacroDefSet> macroDefs = new HashMap<>();
	public HashMap<String, RALCallable> callable = new HashMap<>();

	public Module() {
		
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

	/**
	 * Compiles the module.
	 */
	public void compile(StringBuilder outText, TypeSystem ts) {
		if (installScript != null) {
			ScriptContext scr = new ScriptContext(ts, this, ts.gAny, ts.gAny);
			compile(outText, ts, scr, installScript);
		}

		for (Map.Entry<ScriptIdentifier, RALStatementUR> eventScript : eventScripts.entrySet()) {
			ScriptIdentifier k = eventScript.getKey();
			RALStatementUR v = eventScript.getValue();
			outText.append(" * ");
			RALType.AgentClassifier type = ts.byClassifier(k.classifier);
			outText.append(type.typeName);
			String msgName = type.lookupMSName(k.script, true);
			if (msgName != null) {
				outText.append(":");
				outText.append(msgName);
			}
			outText.append(" ");
			outText.append(k.script);
			outText.append("\n");
			outText.append("scrp ");
			outText.append(k.classifier.family);
			outText.append(" ");
			outText.append(k.classifier.genus);
			outText.append(" ");
			outText.append(k.classifier.species);
			outText.append(" ");
			outText.append(k.script);
			outText.append("\n");
			RALType oOwnr = ts.byClassifier(k.classifier);
			RALType oFrom = ts.gAny;
			RALType override = ts.overrideOwnr.get(k.script);
			if (override != null) {
				oOwnr = override;
				oFrom = oOwnr;
			}
			ScriptContext scr = new ScriptContext(ts, this, oOwnr, oFrom);
			compile(outText, ts, scr, v);
			outText.append("endm\n");
		}

		if (removeScript != null) {
			outText.append("rscr\n");
			ScriptContext scr = new ScriptContext(ts, this, ts.gNull, ts.gAny);
			compile(outText, ts, scr, removeScript);
		}
	}

	private void compile(StringBuilder outText, TypeSystem ts, ScriptContext scr, RALStatementUR v) {
		ScopeContext scope = new ScopeContext(scr);
		RALStatement res = v.resolve(scope);
		res.compile(outText, new CompileContext(scr));
	}
}
