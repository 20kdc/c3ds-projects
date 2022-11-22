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

	/**
	 * Compiles the module's install script.
	 */
	public void compileInstall(OuterCompileContext ctx) {
		TypeSystem ts = ctx.typeSystem;
		if (installScript != null) {
			ScriptContext scr = new ScriptContext(ts, this, ctx.diags, ts.gAny, ts.gAny, ts.gAny, ts.gAny);
			compile(ctx.out, scr, installScript, 0, ctx.debug);
		}
	}

	/**
	 * Compiles the module's event scripts.
	 */
	public void compileEvents(OuterCompileContext ctx) {
		for (Map.Entry<ScriptIdentifier, RALStatementUR> eventScript : eventScripts.entrySet()) {
			ScriptIdentifier k = eventScript.getKey();
			ctx.out.append(" * ");
			RALType.AgentClassifier type = ctx.typeSystem.byClassifier(k.classifier);
			ctx.out.append(type.typeName);
			String msgName = type.lookupMSName(k.script, true);
			if (msgName != null) {
				ctx.out.append(":");
				ctx.out.append(msgName);
			}
			ctx.out.append(" ");
			ctx.out.append(k.script);
			ctx.out.append("\n");
			ctx.out.append(k.toScrpLine());
			ctx.out.append("\n");
			compileEventContents(ctx, k);
			ctx.out.append("endm\n");
		}
	}

	/**
	 * Compiles the module's event scripts to a set of requests.
	 */
	public void compileEventsForInject(LinkedList<String> requests, TypeSystem ts, DiagRecorder diags) {
		for (Map.Entry<ScriptIdentifier, RALStatementUR> eventScript : eventScripts.entrySet()) {
			ScriptIdentifier k = eventScript.getKey();
			StringBuilder outText = new StringBuilder();
			outText.append(k.toScrpLine());
			outText.append('\n');
			compileEventContents(new OuterCompileContext(outText, ts, diags, false), k);
			requests.add(outText.toString());
		}
	}

	/**
	 * Compiles the content of an event script.
	 */
	public void compileEventContents(OuterCompileContext ctx, ScriptIdentifier k) {
		TypeSystem ts = ctx.typeSystem;
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
		ScriptContext scr = new ScriptContext(ts, this, ctx.diags, oOwnr, oFrom, oP1, oP2);
		compile(ctx.out, scr, v, 1, ctx.debug);
	}

	/**
	 * Compiles the module's install script.
	 */
	public void compileRemove(OuterCompileContext ctx) {
		TypeSystem ts = ctx.typeSystem;
		if (removeScript != null) {
			ScriptContext scr = new ScriptContext(ts, this, ctx.diags, ts.gNull, ts.gAny, ts.gAny, ts.gAny);
			compile(ctx.out, scr, removeScript, 1, ctx.debug);
		}
	}

	/**
	 * Compiles the module.
	 */
	public void compile(OuterCompileContext ctx) {
		compileInstall(ctx);
		compileEvents(ctx);
		if (removeScript != null)
			ctx.out.append("rscr\n");
		compileRemove(ctx);
	}

	private void compile(StringBuilder sb, ScriptContext scr, RALStatementUR v, int ii, boolean debug) {
		RALStatement res;
		try {
			ScopeContext scope = new ScopeContext(scr);
			res = v.resolve(scope);
		} catch (Exception ex) {
			scr.diags.error(v.lineNumber, "failed resolving: ", ex);
			return;
		}
		try {
			CodeWriter cw = new CodeWriter(sb, debug);
			cw.indent = ii;
			res.compile(cw, new CompileContext(scr, cw));
		} catch (Exception ex) {
			scr.diags.error(v.lineNumber, "failed writing code: ", ex);
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
