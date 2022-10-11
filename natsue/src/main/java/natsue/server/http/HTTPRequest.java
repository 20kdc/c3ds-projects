/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import natsue.data.babel.PacketReader;
import natsue.log.ILogSource;

/**
 * Just to get this out of the way...
 */
public class HTTPRequest {
	/**
	 * Reads a request header from a socket.
	 * Returns null and sends a response to the given place if an error occurred.
	 */
	public static byte[] readRequestHeader(Socket socket, ILogSource log, IHTTPHandler.Client rsp, int firstByte, int maxRequestSize, int requestTotalMs) throws IOException {
		try {
			long endTime = System.currentTimeMillis() + requestTotalMs;
			byte[] requestBuffer = new byte[maxRequestSize];
			int keepAlive = requestTotalMs;
			int len = 0;
			if (firstByte != -1)
				requestBuffer[len++] = (byte) firstByte;
			while (len < maxRequestSize) {
				long remainingTime = endTime - System.currentTimeMillis();
				if (remainingTime <= 0)
					throw new SocketTimeoutException();
				if (PacketReader.readWithTimeout(socket, keepAlive, requestBuffer, len, 1) <= 0) {
					rsp.httpResponse("400 Bad Request", false, "The request was not properly terminated.");
					return null;
				}
				len++;
				if ((len >= 4) &&
					(requestBuffer[len - 4] == '\r') &&
					(requestBuffer[len - 3] == '\n') &&
					(requestBuffer[len - 2] == '\r') &&
					(requestBuffer[len - 1] == '\n')) {
					byte[] resBuffer = new byte[len];
					System.arraycopy(requestBuffer, 0, resBuffer, 0, len);
					return resBuffer;
				}
			}
			rsp.httpResponse("413 Request Too Large", false, "The request was too large.");
		} catch (SocketTimeoutException ste) {
			rsp.httpResponse("408 Request Timeout", false, "The request was not sent in a timely manner.");
		} catch (Exception ex) {
			log.log(ex);
			StringWriter sb = new StringWriter();
			PrintWriter pb = new PrintWriter(sb);
			ex.printStackTrace(pb);
			pb.flush();
			rsp.httpResponse("500 Internal Server Error", false, sb.toString());
		}
		return null;
	}
}
