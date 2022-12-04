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
import rals.stmt.RALStatement;
import rals.types.*;

/**
 * Represents resolved scripts (as opposed to the unresolved script soup).
 */
public class Scripts {
	public final TypeSystem typeSystem;
	public final DiagRecorder diags;

	public RALStatement installScript;
	public HashMap<ScriptIdentifier, RALStatement> eventScripts = new HashMap<>();
	public RALStatement removeScript;

	public Scripts(TypeSystem ts, DiagRecorder d) {
		typeSystem = ts;
		diags = d;
	}

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
			RALType.AgentClassifier type = typeSystem.byClassifier(k.classifier);
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
	 * Compiles a section of this module to a set of requests.
	 */
	public void compileSectionForInject(LinkedList<String> queuedRequests, ScriptSection k) {
		RALStatement stmt;
		switch (k) {
		case Install:
			stmt = installScript;
			break;
		case Events:
			for (Map.Entry<ScriptIdentifier, RALStatement> eventScript : eventScripts.entrySet()) {
				ScriptIdentifier k2 = eventScript.getKey();
				StringBuilder outText = new StringBuilder();
				outText.append(k2.toScrpLine());
				outText.append('\n');
				compileEventContents(new OuterCompileContext(outText, false), k2);
				queuedRequests.add(outText.toString());
			}
			return;
		case Remove:
			stmt = removeScript;
			break;
		default:
			throw new RuntimeException("Unknown GlobalScriptKind " + k);
		}
		StringBuilder outText = new StringBuilder();
		outText.append("execute\n");
		if (stmt != null)
			compile(new OuterCompileContext(outText, false), stmt, 0);
		queuedRequests.add(outText.toString());
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
		CodeWriter cw = new CodeWriter(ctx.out, ctx.debug);
		cw.indent = ii;
		v.compile(cw, new CompileContext(typeSystem, this, diags, cw));
	}
}
