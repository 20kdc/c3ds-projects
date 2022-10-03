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
import rals.stmt.RALBlock;
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
public class Scripts {
	public RALBlock installScript;
	public HashMap<ScriptIdentifier, RALStatementUR> eventScripts = new HashMap<>();
	public RALBlock removeScript;

	public HashMap<String, MacroDefSet> macroDefs = new HashMap<>();
	public HashMap<String, RALCallable> callable = new HashMap<>();

	public Scripts() {
		
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
	 * Compiles the module's install script.
	 */
	public void compileInstall(StringBuilder outText, TypeSystem ts) {
		if (installScript != null) {
			ScriptContext scr = new ScriptContext(ts, this, ts.gAny, ts.gAny, ts.gAny, ts.gAny);
			compile(outText, ts, scr, installScript, 0);
		}
	}

	/**
	 * Compiles the module's event scripts.
	 */
	public void compileEvents(StringBuilder outText, TypeSystem ts) {
		for (Map.Entry<ScriptIdentifier, RALStatementUR> eventScript : eventScripts.entrySet()) {
			ScriptIdentifier k = eventScript.getKey();
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
			compileEventContents(outText, ts, k);
			outText.append("endm\n");
		}
	}

	/**
	 * Compiles the content of an event script.
	 */
	public void compileEventContents(StringBuilder outText, TypeSystem ts, ScriptIdentifier k) {
		RALStatementUR v = eventScripts.get(k);
		RALType oOwnr = ts.byClassifier(k.classifier);
		RALType oFrom = ts.gAny;
		RALType oP1 = ts.gAny;
		RALType oP2 = ts.gAny;
		RALType override = ts.overrideOwnr.get(k.script);
		if (override != null) {
			oOwnr = override;
			oFrom = oOwnr;
		}
		ScriptContext scr = new ScriptContext(ts, this, oOwnr, oFrom, oP1, oP2);
		compile(outText, ts, scr, v, 1);
	}

	/**
	 * Compiles the module's install script.
	 */
	public void compileRemove(StringBuilder outText, TypeSystem ts) {
		if (removeScript != null) {
			ScriptContext scr = new ScriptContext(ts, this, ts.gNull, ts.gAny, ts.gAny, ts.gAny);
			compile(outText, ts, scr, removeScript, 1);
		}
	}

	/**
	 * Compiles the module.
	 */
	public void compile(StringBuilder outText, TypeSystem ts) {
		compileInstall(outText, ts);
		compileEvents(outText, ts);
		if (removeScript != null)
			outText.append("rscr\n");
		compileRemove(outText, ts);
	}

	private void compile(StringBuilder outText, TypeSystem ts, ScriptContext scr, RALStatementUR v, int ii) {
		ScopeContext scope = new ScopeContext(scr);
		RALStatement res = v.resolve(scope);
		CodeWriter cw = new CodeWriter(outText);
		cw.indent = ii;
		res.compile(cw, new CompileContext(scr, cw));
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