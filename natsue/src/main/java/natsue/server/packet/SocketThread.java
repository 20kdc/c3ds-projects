/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import natsue.config.Config;
import natsue.data.babel.PacketReader;
import natsue.data.babel.PacketWriter;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.session.ISessionClient;
import natsue.server.http.HTTPRequest;
import natsue.server.http.IHTTPHandler;
import natsue.server.session.BaseSessionState;

/**
 * Thread for a given client.
 */
public class SocketThread extends Thread implements ILogSource, ISessionClient, IHTTPHandler.Client {
	public final Socket socket;
	public final QuotaManager quota;
	private InputStream socketInput;
	private OutputStream socketOutput;
	private final Object sendPacketLock = new Object();
	private final ILogProvider logParent;
	public final Config config;
	public BaseSessionState sessionState;
	public final Function<SocketThread, BaseSessionState> initialSessionStateBuilder;
	public final IHTTPHandler initialHandler;
	public long myUIN;

	public SocketThread(Socket skt, QuotaManager qm, Function<SocketThread, BaseSessionState> iSessionStateBuilder, IHTTPHandler iHandler, ILogProvider ilp, Config stc) {
		socket = skt;
		quota = qm;
		logParent = ilp;
		initialSessionStateBuilder = iSessionStateBuilder;
		initialHandler = iHandler;
		config = stc;
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	@Override
	public void setSessionState(BaseSessionState session) {
		sessionState = session;
	}

	@Override
	public void sendPacket(byte[] packet) throws IOException {
		synchronized (sendPacketLock) {
			socketOutput.write(packet);
		}
	}

	@Override
	public boolean logFailedAuth() {
		return config.logFailedAuthentication.getValue();
	}

	@Override
	public boolean logPings() {
		return config.logPings.getValue();
	}

	@Override
	public void forceDisconnect(boolean sync) {
		try {
			// It's worth noting Java defines this as a thread-safe thing to do.
			// It'll also do exactly what's wanted of it - causing exceptions that will terminate the reader.
			socket.close();
		} catch (Exception ex) {
			// Do not care
		}
		if (sync) {
			if (Thread.currentThread() == this) {
				// oops
				log(new Throwable("NOT AN EXCEPTION, BUT DEFINITELY AN ERROR: SYNCHRONOUS CONNECTION SHOOTDOWN TARGETTING CURRENT THREAD").fillInStackTrace());
			} else {
				try {
					// Join implies sessionState.logout(); will happen.
					join();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			// Set initial settings
			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			socket.setKeepAlive(true);
			setName("Natsue-" + socket.getRemoteSocketAddress());
			if (config.logAllConnections.getValue())
				log("Accepted");
			// Get first byte (as HTTP connection check)
			int firstByte = -1;
			byte[] tmpHttpChk = new byte[1];
			if (PacketReader.readWithTimeout(socket, config.initialNoDataShutdownTime.getAsClampedMs(), tmpHttpChk, 0, 1) != 1) {
				// fine then, be that way
				return;
			}
			firstByte = tmpHttpChk[0] & 0xFF;
			if (firstByte != 0x25) {
				// If this isn't a handshake packet, then this is not a Babel connection (or at least a normal one).
				// Assume it to be HTTP.
				handleHTTPConnection(firstByte);
				return;
			}
			// Confirmed to be a Babel connection.
			sessionState = initialSessionStateBuilder.apply(this);
			// This is the main loop!
			int keepAlive = config.manualKeepAliveTime.getAsClampedMs();
			while (sessionState != null) {
				byte[] header = null;
				try {
					header = PacketReader.readPacketHeader(socket, keepAlive, firstByte);
					firstByte = -1;
					if (header == null)
						break;
				} catch (SocketTimeoutException ste) {
					// keepAlive logic triggers, retry
					// log("sending KA Packet");
					sendPacket(PacketWriter.writeDummy());
					continue;
				}
				BaseCTOS packet = PacketReader.readPacket(config.messages, header, socketInput);
				if (config.logAllIncomingPackets.getValue())
					log(packet.toString());
				sessionState.handlePacket(packet);
			}
			// nevermind then
		} catch (Exception ex) {
			log(ex);
		} finally {
			try {
				synchronized (sendPacketLock) {
					// Note that we hold sendPacketLock through the logout process. 
					if (sessionState != null)
						sessionState.logout();
					socket.close();
				}
			} catch (Exception ex2) {
				// Deliberately ignored - we're closing the socket.
			}
			if (config.logAllConnections.getValue())
				log("Closed");
			quota.socketEnd(socket);
		}
	}

	private void handleHTTPConnection(int firstByte) throws IOException {
		// Can just make this immediately stop
		if (!config.httpRequestsEnabled.getValue())
			return;
		// Ok, take it seriously
		byte[] request = HTTPRequest.readRequestHeader(socket, this, this, firstByte, config.httpRequestMaxLength.getValue(), config.httpRequestTime.getValue());
		if (request != null) {
			String requestStr = new String(request, 0, request.length, StandardCharsets.UTF_8);
			int idx1 = requestStr.indexOf('\r');
			String requestLine = requestStr.substring(0, idx1);
			try {
				initialHandler.handleHTTP(requestLine, this);
			} catch (Exception ex) {
				log(ex);
				StringWriter sb = new StringWriter();
				PrintWriter pb = new PrintWriter(sb);
				ex.printStackTrace(pb);
				pb.flush();
				httpResponse("500 Internal Server Error", false, sb.toString());
			}
		}
	}
	@Override
	public void httpResponse(String status, boolean head, String contentType, byte[] body) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("HTTP/1.1 ");
		sb.append(status);
		sb.append("\r\nContent-Type: ");
		sb.append(contentType);
		sb.append("\r\nContent-Length: ");
		sb.append(body.length);
		sb.append("\r\nConnection: close\r\n\r\n");
		socketOutput.write(sb.toString().getBytes(StandardCharsets.UTF_8));
		if (!head) {
			socketOutput.write(body);
		}
		socketOutput.flush();
		PacketReader.linger(socket, config.httpRequestFakeLingerTime.getAsClampedMs());
	}

	@Override
	public boolean isLocal() {
		return socket.getInetAddress().isLoopbackAddress();
	}
}
