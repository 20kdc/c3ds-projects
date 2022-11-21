/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Language server logic.
 */
public class LanguageServer implements ILSPCore {
	@Override
	public void handleNotification(String method, JSONObject params, LSPBaseProtocolLoop sendback) throws IOException {
		if (method.equals("textDocument/didOpen")) {
			JSONObject actualParams = params.getJSONObject("textDocument");
			String uri = actualParams.getString("uri");
			JSONObject diagsUpdate = new JSONObject();
			diagsUpdate.put("uri", uri);
			JSONArray diagsContent = new JSONArray();
			diagsContent.put(new JSONObject("{\"range\":{\"start\":{\"line\":1,\"character\":0},\"end\":{\"line\":1,\"character\":0}},\"message\":\"Hai\"}"));
			diagsUpdate.put("diagnostics", diagsContent);
			sendback.sendNotification("textDocument/publishDiagnostics", diagsUpdate);
		}
	}

	@Override
	public JSONObject handleRequest(String method, JSONObject params, LSPBaseProtocolLoop sendback) throws LSPErrorException, IOException {
		if (method.equals("initialize")) {
			// Unfortunately, Microsoft treats LSP as a "living standard", which is a really fancy way of saying
			//  that they'll change around things and hide the old specs in commit history.
			// So I really don't know what the "version handshake" that they refer to *was*.
			JSONObject res = new JSONObject();
			JSONObject caps = new JSONObject();
			// Sync via sending full contents
			caps.put("textDocumentSync", 1);
			JSONObject diag = new JSONObject();
			diag.put("interFileDependencies", true);
			diag.put("workspaceDiagnostics", false);
			caps.put("diagnosticProvider", diag);
			res.put("capabilities", caps);
			return res;
		}
		throw new LSPMethodNotFoundException(method);
	}
}
