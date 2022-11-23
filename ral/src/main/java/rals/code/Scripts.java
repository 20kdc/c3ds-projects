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
import rals.expr.RALExprSlice;
import rals.stmt.RALStatement;
import rals.types.RALType;
import rals.types.ScriptIdentifier;
import rals.types.TypeSystem;

/**
 * Represents resolved scripts (as opposed to the unresolved script soup).
 */
public class Scripts {
	public RALStatement installScript;
	public HashMap<ScriptIdentifier, RALStatement> eventScripts = new HashMap<>();
	public RALStatement removeScript;

	/**
	 * Compiles the module's install script.
	 */
	public void compileInstall(OuterCompileContext ctx) {
		if (installScript != null)
			compile(ctx, installScript, 0);
	}

	/**
	 * Compiles the module's event scripts.
	 */
	public void compileEvents(OuterCompileContext ctx) {
		for (Map.Entry<ScriptIdentifier, RALStatement> eventScript : eventScripts.entrySet()) {
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
		for (Map.Entry<ScriptIdentifier, RALStatement> eventScript : eventScripts.entrySet()) {
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
		RALStatement v = eventScripts.get(k);
		compile(ctx, v, 1);
	}

	/**
	 * Compiles the module's install script.
	 */
	public void compileRemove(OuterCompileContext ctx) {
		if (removeScript != null)
			compile(ctx, removeScript, 1);
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

	private void compile(OuterCompileContext ctx, RALStatement v, int ii) {
		try {
			CodeWriter cw = new CodeWriter(ctx.out, ctx.debug);
			cw.indent = ii;
			v.compile(cw, new CompileContext(ctx.typeSystem, this, ctx.diags, cw));
		} catch (Exception ex) {
			ctx.diags.error(v.extent, "failed writing code: ", ex);
		}
	}
}
