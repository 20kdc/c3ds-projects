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
import java.net.Socket;
import java.util.function.Function;

import natsue.data.babel.PacketReader;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.session.ISessionClient;
import natsue.server.session.BaseSessionState;

/**
 * Thread for a given client.
 */
public class SocketThread extends Thread implements ILogSource, ISessionClient {
	public final Socket socket;
	private InputStream socketInput;
	private OutputStream socketOutput;
	private final Object sendPacketLock = new Object();
	public final ILogProvider log;
	public final PacketReader packetReader;
	public final SocketThreadConfig config;
	public BaseSessionState sessionState;
	public final Function<SocketThread, BaseSessionState> initialSessionStateBuilder;
	public long myUIN;

	public SocketThread(Socket skt, Function<SocketThread, BaseSessionState> iSessionStateBuilder, ILogProvider ilp, PacketReader pr, SocketThreadConfig stc) {
		socket = skt;
		log = ilp;
		packetReader = pr;
		initialSessionStateBuilder = iSessionStateBuilder;
		config = stc;
	}

	@Override
	public void setSessionState(BaseSessionState session) {
		sessionState = session;
	}

	/**
	 * Sends a packet, holding a lock to make sure nothing gets in anything else's way.
	 */
	public void sendPacket(byte[] packet) throws IOException {
		synchronized (sendPacketLock) {
			socketOutput.write(packet);
		}
	}

	@Override
	public boolean logFailedAuth() {
		return config.logFailedAuthentication;
	}

	@Override
	public void log(String source, String text) {
		log.log(this + ":" + source, text);
	}

	@Override
	public void log(String source, Throwable ex) {
		log.log(this + ":" + source, ex);
	}

	@Override
	public void run() {
		try {
			// Set initial settings
			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			socket.setKeepAlive(true);
			setName("Natsue-" + socket.getRemoteSocketAddress());
			if (config.logAllConnections)
				logTo(log, "Accepted");
			sessionState = initialSessionStateBuilder.apply(this);
			BaseCTOS packet = packetReader.readPacket(socketInput);
			// This is the main loop!
			while (sessionState != null) {
				packet = packetReader.readPacket(socketInput);
				if (packet == null)
					break;
				sessionState.handlePacket(packet);
			}
			// nevermind then
		} catch (Exception ex) {
			logTo(log, ex);
		} finally {
			if (sessionState != null)
				sessionState.logout();
			try {
				socket.close();
			} catch (Exception ex2) {
				// Deliberately ignored - we're closing the socket.
			}
			if (config.logAllConnections)
				logTo(log, "Closed");
		}
	}
}
