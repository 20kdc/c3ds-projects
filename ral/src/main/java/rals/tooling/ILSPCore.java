/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.io.IOException;

import org.json.JSONObject;

/**
 * Wraps the logic because it's going to be this big class...
 */
public interface ILSPCore {
	/**
	 * Handles an LSP notification.
	 */
	public void handleNotification(String method, JSONObject params, LSPBaseProtocolLoop sendback) throws IOException;

	/**
	 * Handles an LSP request. Must return the response or throw an LSPErrorException.
	 */
	public JSONObject handleRequest(String method, JSONObject params, LSPBaseProtocolLoop sendback) throws LSPErrorException, IOException;

	@SuppressWarnings("serial")
	public static class LSPMethodNotFoundException extends LSPErrorException {
		public LSPMethodNotFoundException(String m) {
			super(-32601, "No such method: " + m);
		}
	}

	@SuppressWarnings("serial")
	public static class LSPErrorException extends Exception {
		public final int lspErrorCode;
		public final String lspErrorText;
		public LSPErrorException(int code, String text) {
			super(text);
			lspErrorCode = code;
			lspErrorText = text;
		}
	}
}
