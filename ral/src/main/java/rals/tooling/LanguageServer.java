/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.io.File;
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

	public LanguageServer(File sl, boolean dbgMode) {
		stdLib = docRepo.getDocPath(sl);
		debugMode = dbgMode;
	}

	private void figureOutWhatToDoWithDiag(LinkedList<LSPDiag> finalDiagSet, HashSet<IDocPath> includeWarnings, IDocPath docPath, Diag d) {
		// Ok, so, first, trace through the frames to find the first point where our target file is involved.
		// This might require lex/parse errors get special treatment though.
		int firstTargetFileFrame = -1;
		for (int frameIndex = 0; frameIndex < d.frames.length; frameIndex++) {
			if (d.frames[frameIndex].file.docPath == docPath) {
				firstTargetFileFrame = frameIndex;
				break;
			}
		}
		if (firstTargetFileFrame != -1) {
			// It is involved, so escalate up to there.
			StringBuilder sb = new StringBuilder();
			for (int frameAppend = 0; frameAppend <= firstTargetFileFrame; frameAppend++) {
				sb.append(d.frames[frameAppend]);
				sb.append(": ");
			}
			sb.append(d.shortText);
			finalDiagSet.add(new LSPDiag(d.kind, d.frames[firstTargetFileFrame], sb.toString()));
			return;
		}
		// If it's not an error, we don't care from this point on.
		if (d.kind != Kind.Error)
			return;
		// Alright, now treat this like a parse error.
		SrcRange location = d.frames[0];
		IDocPath originalSource = location.file.docPath;
		// set checkMe to the location of the include
		SrcPos checkMe = location.start;
		while (checkMe.file.includedFrom != null)
			checkMe = checkMe.file.includedFrom;
		// only show one
		if (!includeWarnings.contains(originalSource)) {
			includeWarnings.add(originalSource);
			String ost = location.start + ": " + d.shortText;
			finalDiagSet.add(new LSPDiag(Diag.Kind.Error, checkMe.toRange(), ost));
		}
	}

	public LSPDiag[] getDiagnostics(IDocPath docPath) {
		SrcPosFile docPathSPF = new SrcPosFile(null, docPath, docPath.getRootShortName());
		try {
			ActualHCMRecorder hcm = new ActualHCMRecorder(docPath);
			IncludeParseContext ipc = new IncludeParseContext(hcm, false);
			// Need the builtins and such
			ipc.searchPaths.add(stdLib);
			Parser.findParseFile(ipc, null, "std/compiler_helpers.ral");

			// Actually compile this...
			Parser.parseFileAt(ipc, docPathSPF);
			Scripts scr = ipc.module.resolve(ipc.typeSystem, ipc.diags, ipc.hcm);
			// This shouldn't really be necessary, but do it to be safe.
			scr.compile(new OuterCompileContext(new StringBuilder(), DebugType.None));

			// Compilation completed, sort all the guts out
			docHCM.put(docPath, hcm.compile(ipc));

			LinkedList<LSPDiag> finalDiagSet = new LinkedList<>();
			HashSet<IDocPath> includeWarnings = new HashSet<>();
			for (Diag d : ipc.diags.diagnostics) {
				figureOutWhatToDoWithDiag(finalDiagSet, includeWarnings, docPath, d);
			}
			return finalDiagSet.toArray(new LSPDiag[0]);
		} catch (Exception ex) {
			ex.printStackTrace();
			String msg = "diagnostics exception: " + ex.toString();
			// whoopsie!
			return new LSPDiag[] {
				new LSPDiag(Diag.Kind.Error, new SrcPos(docPathSPF, 0, 0, 0).toRange(), msg)
			};
		}
	}

	public void regenDiagnostics(String uri, LSPBaseProtocolLoop sendback) throws IOException {
		JSONObject diagsUpdate = new JSONObject();
		diagsUpdate.put("uri", uri);
		JSONArray diagsContent = new JSONArray();
		LSPDiag[] gd = getDiagnostics(docRepo.getDocPath(uri));
		for (LSPDiag d : gd)
			diagsContent.put(d.toLSPDiagnostic());
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
			caps.put("definitionProvider", true);
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
					if (hd != null && hd.defInfo != null && hd.defInfo.docComment != null)
						hoverText += "\n" + hd.defInfo.docComment;
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
						if (hd2.defInfo != null && hd2.defInfo.docComment != null)
							test.put("documentation", hd2.defInfo.docComment);
						items.put(test);
					}
					return items;
				}
			}
			return null;
		} else if (method.equals("textDocument/definition")) {
			JSONObject ident = params.getJSONObject("textDocument");
			String givenURI = ident.getString("uri");
			IDocPath docPath = docRepo.getDocPath(givenURI);
			SrcPosUntranslated spu = new SrcPosUntranslated(docPath, params.getJSONObject("position"));
			HCMStorage hcm = docHCM.get(docPath);
			if (hcm != null) {
				HCMStorage.HoverData hd = hcm.getHoverData(spu);
				if ((hd != null) && (hd.defInfo != null) && (hd.defInfo.srcRange != null)) {
					SrcRange range = hd.defInfo.srcRange;
					JSONObject test = new JSONObject();
					test.put("uri", hd.defInfo.srcRange.file.docPath.toLSPURI());
					test.put("range", range.toLSPRange());
					return test;
				}
			}
			return null;
		}
		throw new LSPMethodNotFoundException(method);
	}
}
