/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.json.JSONObject;
import org.json.JSONTokener;

import rals.tooling.ILSPCore.LSPErrorException;

/**
 * Language server base protocol stuff.
 */
public class LSPBaseProtocolLoop {
	public boolean hasReceivedShutdown = false;
	public final PrintWriter err;
	public final ILSPCore core;
	public final DataInputStream dis;
	public final PrintStream out;

	public LSPBaseProtocolLoop(ILSPCore c) {
		dis = new DataInputStream(System.in);
		err = new PrintWriter(System.err);
		out = System.out;
		core = c;
	}

	/**
	 * Runs the language server.
	 */
	public void run() throws IOException {
		err.println(" -- LanguageServer started @ " + new Date() + " -- ");
		err.flush();
		while (true) {
			// check for EOF
			int cl = -1;
			while (true) {
				// doesn't do unicode, but these headers have no reason to care
				// importantly, this preserves stream consistency, which was breaking things w/ BufferedReader
				@SuppressWarnings("deprecation")
				String ln = dis.readLine();
				if (ln.equals(""))
					break;
				int colon = ln.indexOf(':');
				if (colon == -1)
					continue;
				String preColon = ln.substring(0, colon).trim();
				String postColon = ln.substring(colon + 1).trim();
				if (preColon.equalsIgnoreCase("content-length"))
					cl = Integer.parseInt(postColon);
			}
			if (cl <= 0)
				throw new RuntimeException("LSP failure due to lack of content length.");

			byte[] data = new byte[cl];
			dis.readFully(data);
			String dataStr = new String(data, StandardCharsets.UTF_8);

			// dump input
			err.print("IN: ");
			err.println(dataStr);
			err.flush();

			// continue parsing
			JSONTokener jt = new JSONTokener(dataStr);
			JSONObject msg = new JSONObject(jt);

			// continue
			if (msg.has("id")) {
				Object requestResponseID = msg.get("id");
				if (msg.has("method")) {
					// Request
					if (hasReceivedShutdown) {
						sendError(out, -32600, requestResponseID, "Shutdown state");
					} else {
						String m = msg.getString("method");
						if (m.equals("shutdown")) {
							// handled internally
							hasReceivedShutdown = true;
						} else {
							try {
								Object res = core.handleRequest(m, msg.has("params") ? msg.getJSONObject("params") : null, this);
								JSONObject rsp = new JSONObject();
								rsp.put("jsonrpc", "2.0");
								rsp.put("id", requestResponseID);
								rsp.put("result", res);
								sendObj(out, rsp);
							} catch (LSPErrorException le) {
								sendError(out, le.lspErrorCode, requestResponseID, le.lspErrorText);
							} catch (Exception ex) {
								ex.printStackTrace();
								sendError(out, -32603, requestResponseID, ex.toString());
							}
						}
					}
				} else {
					// Response (ignore)
				}
			} else if (msg.has("method")) {
				// Notification
				String m = msg.getString("method");
				if (m.equals("exit")) {
					// handled internally
					System.exit(hasReceivedShutdown ? 0 : 1);
				} else {
					try {
						core.handleNotification(m, msg.has("params") ? msg.getJSONObject("params") : null, this);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	private void sendError(PrintStream out, int code, Object id, String string) throws IOException {
		JSONObject rsp = new JSONObject();
		rsp.put("jsonrpc", "2.0");
		rsp.put("id", id);
		JSONObject err = new JSONObject();
		rsp.put("error", err);
		err.put("code", code);
		err.put("message", string);
		sendObj(out, rsp);
	}

	public void sendNotification(String method, JSONObject par) throws IOException {
		JSONObject rsp = new JSONObject();
		rsp.put("jsonrpc", "2.0");
		rsp.put("method", method);
		if (par != null)
			rsp.put("params", par);
		sendObj(out, rsp);
	}

	private void sendObj(PrintStream out, JSONObject rsp) throws IOException {
		// dump output
		err.print("OUT: ");
		rsp.write(err, 1, 0);
		err.println();
		err.flush();
		// send
		byte[] data = rsp.toString().getBytes(StandardCharsets.UTF_8);
		out.print("Content-Length: " + data.length + "\r\n\r\n");
		out.flush();
		out.write(data);
		out.flush();
	}
}
