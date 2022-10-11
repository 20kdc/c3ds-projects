/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * For internal HTTP services
 */
public interface IHTTPHandler {
	default void handleHTTP(String line, Client r) throws IOException {
		String[] parts = line.split(" ");
		if (parts.length < 2) {
			r.httpResponse("400 Bad Request", false, "< 2 request components");
			return;
		}
		if (parts[0].equalsIgnoreCase("GET")) {
			handleHTTPGet(parts[1], false, r);
		} else if (parts[0].equalsIgnoreCase("HEAD")) {
			handleHTTPGet(parts[1], true, r);
		} else {
			r.httpResponse("405 Method Not Allowed", false, "The method " + parts[0] + " was unrecognized.");
			return;
		}
	}

	void handleHTTPGet(String url, boolean head, Client r) throws IOException;

	interface Client {

		default void httpOk(boolean head, String contentType, String body) throws IOException {
			httpResponse("200 OK", head, contentType, body);
		}

		default void httpOk(boolean head, String contentType, byte[] body) throws IOException {
			httpResponse("200 OK", head, contentType, body);
		}

		default void httpResponse(String status, boolean head, String body) throws IOException {
			httpResponse(status, head, "text/plain", body.getBytes(StandardCharsets.UTF_8));
		}

		default void httpResponse(String status, boolean head, String contentType, String body) throws IOException {
			httpResponse(status, head, contentType, body.getBytes(StandardCharsets.UTF_8));
		}
	
		void httpResponse(String status, boolean head, String contentType, byte[] body) throws IOException;
	}
}
