/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import rals.code.*;
import rals.diag.*;
import rals.diag.Diag.Kind;
import rals.hcm.*;
import rals.hcm.HCMStorage.HoverData;
import rals.parser.*;

/**
 * Language server logic.
 */
public class LanguageServer implements ILSPCore {
	public final LSPDocRepo docRepo = new LSPDocRepo();
	public final HashMap<IDocPath, HCMStorage> docHCM = new HashMap<>();
	public final IDocPath stdLib;
	public final boolean debugMode;

	public LanguageServer(IDocPath sl, boolean dbgMode) {
		stdLib = sl;
		debugMode = dbgMode;
	}

	public Diag[] getDiagnostics(IDocPath docPath) {
		SrcPosFile docPathSPF = new SrcPosFile(null, docPath, docPath.getRootShortName());
		try {
			ActualHCMRecorder hcm = new ActualHCMRecorder(docPath);
			IncludeParseContext ipc = new IncludeParseContext(hcm, false);
			// Need the builtins and such
			ipc.searchPaths.add(stdLib);
			Parser.findParseFile(ipc, null, "std/compiler_helpers.ral", null);

			// Actually compile this...
			Parser.parseFileAt(ipc, docPathSPF);
			Scripts scr = ipc.module.resolve(ipc.typeSystem, ipc.diags, ipc.hcm);
			// This shouldn't really be necessary, but do it to be safe.
			scr.compile(new OuterCompileContext(new StringBuilder(), ipc.typeSystem, ipc.diags, false));

			// Compilation completed, sort all the guts out
			docHCM.put(docPath, hcm.compile(ipc));

			LinkedList<Diag> finalDiagSet = new LinkedList<>();
			HashSet<IDocPath> includeWarnings = new HashSet<>();
			for (Diag d : ipc.diags.diagnostics) {
				if (d.location.file.docPath.equals(docPath)) {
					finalDiagSet.add(d);
				} else if (d.kind == Kind.Error) {
					IDocPath originalSource = d.location.file.docPath;
					String originalSourceName = d.location.file.shortName;
					// set checkMe to the location of the include
					SrcPos checkMe = d.location.start;
					while (checkMe.file.includedFrom != null)
						checkMe = checkMe.file.includedFrom;
					// only show one
					if (!includeWarnings.contains(originalSource)) {
						includeWarnings.add(originalSource);
						String ost = originalSourceName + " contains errors";
						finalDiagSet.add(new Diag(Diag.Kind.Error, checkMe, ost, ost));
					}
				}
			}
			return finalDiagSet.toArray(new Diag[0]);
		} catch (Exception ex) {
			ex.printStackTrace();
			String msg = "diagnostics exception: " + ex.toString();
			// whoopsie!
			return new Diag[] {
				new Diag(Diag.Kind.Error, new SrcPos(docPathSPF, 0, 0, 0), msg, msg)
			};
		}
	}

	public void regenDiagnostics(String uri, LSPBaseProtocolLoop sendback) throws IOException {
		JSONObject diagsUpdate = new JSONObject();
		diagsUpdate.put("uri", uri);
		JSONArray diagsContent = new JSONArray();
		Diag[] gd = getDiagnostics(docRepo.getDocPath(uri));
		for (Diag d : gd) {
			JSONObject diagJ = new JSONObject();
			diagJ.put("range", d.location.toLSPRange());
			// lite-xl needs this to not malfunction
			diagJ.put("severity", 1);
			diagJ.put("message", d.shortText);
			diagsContent.put(diagJ);
		}
		diagsUpdate.put("diagnostics", diagsContent);
		sendback.sendNotification("textDocument/publishDiagnostics", diagsUpdate);
	}

	@Override
	public void handleNotification(String method, JSONObject params, LSPBaseProtocolLoop sendback) throws IOException {
		if (method.equals("textDocument/didOpen")) {
			JSONObject actualParams = params.getJSONObject("textDocument");
			String givenURI = actualParams.getString("uri");
			docRepo.storeShadow(docRepo.getDocPath(givenURI), actualParams.getString("text"));
			regenDiagnostics(givenURI, sendback);
		} else if (method.equals("textDocument/didChange")) {
			JSONObject ident = params.getJSONObject("textDocument");
			String givenURI = ident.getString("uri");
			JSONArray changes = params.getJSONArray("contentChanges");
			String anyFullContent = null;
			for (int i = 0; i < changes.length(); i++) {
				JSONObject change = changes.getJSONObject(i);
				if (!change.has("range"))
					anyFullContent = change.getString("text");
			}
			if (anyFullContent != null) {
				docRepo.storeShadow(docRepo.getDocPath(givenURI), anyFullContent);
				regenDiagnostics(givenURI, sendback);
			}
		} else if (method.equals("textDocument/didClose")) {
			JSONObject ident = params.getJSONObject("textDocument");
			String givenURI = ident.getString("uri");
			IDocPath docPath = docRepo.getDocPath(givenURI);
			docRepo.storeShadow(docPath, null);
			docHCM.remove(docPath);
		}
	}

	@Override
	public Object handleRequest(String method, JSONObject params, LSPBaseProtocolLoop sendback) throws LSPErrorException, IOException {
		if (method.equals("initialize")) {
			// Unfortunately, Microsoft treats LSP as a "living standard", which is a really fancy way of saying
			//  that they'll change around things and hide the old specs in commit history.
			// So I really don't know what the "version handshake" that they refer to *was*.
			JSONObject res = new JSONObject();
			JSONObject caps = new JSONObject();
			// Sync via sending full contents
			caps.put("textDocumentSync", 1);
			// diagnostic provider
			JSONObject diag = new JSONObject();
			diag.put("interFileDependencies", true);
			diag.put("workspaceDiagnostics", false);
			caps.put("diagnosticProvider", diag);
			// ...
			caps.put("hoverProvider", true);
			// completion provider
			JSONObject comp = new JSONObject();
			caps.put("completionProvider", comp);
			// done
			res.put("capabilities", caps);
			return res;
		} else if (method.equals("textDocument/hover")) {
			JSONObject ident = params.getJSONObject("textDocument");
			String givenURI = ident.getString("uri");
			IDocPath docPath = docRepo.getDocPath(givenURI);
			SrcPosUntranslated spu = new SrcPosUntranslated(docPath, params.getJSONObject("position"));
			HCMStorage hcm = docHCM.get(docPath);
			if (hcm != null) {
				String hoverText = null;
				HCMStorage.HoverData hd = null;
				// Text has to be done in this block
				try {
					if (debugMode) {
						hoverText = hcm.hoverDebugPrefix(spu);
						hd = hcm.getHoverData(spu);
						if (hd != null)
							hoverText += hd.text;
					} else {
						hd = hcm.getHoverData(spu);
						if (hd != null)
							hoverText = hd.text;
					}
				} catch (Exception ex) {
					hoverText = ex.toString();
				}
				// Rest can be done here
				if (hoverText != null) {
					JSONObject test = new JSONObject();
					test.put("contents", hoverText);
					return test;
				}
			}
			return null;
		} else if (method.equals("textDocument/completion")) {
			JSONObject ident = params.getJSONObject("textDocument");
			String givenURI = ident.getString("uri");
			IDocPath docPath = docRepo.getDocPath(givenURI);
			SrcPosUntranslated spu = new SrcPosUntranslated(docPath, params.getJSONObject("position"));
			HCMStorage hcm = docHCM.get(docPath);
			if (hcm != null) {
				Map<String, HCMStorage.HoverData> hd = hcm.getCompletion(spu);
				if (hd != null) {
					JSONArray items = new JSONArray();
					for (Map.Entry<String, HCMStorage.HoverData> ent : hd.entrySet()) {
						JSONObject test = new JSONObject();
						test.put("label", ent.getKey());
						HoverData hd2 = ent.getValue();
						test.put("detail", hd2.text);
						items.put(test);
					}
					return items;
				}
			}
			return null;
		}
		throw new LSPMethodNotFoundException(method);
	}
}
