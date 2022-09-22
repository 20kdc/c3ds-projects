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

import natsue.config.Config;
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
	private final ILogProvider logParent;
	public final Config config;
	public BaseSessionState sessionState;
	public final Function<SocketThread, BaseSessionState> initialSessionStateBuilder;
	public long myUIN;

	public SocketThread(Socket skt, Function<SocketThread, BaseSessionState> iSessionStateBuilder, ILogProvider ilp, Config stc) {
		socket = skt;
		logParent = ilp;
		initialSessionStateBuilder = iSessionStateBuilder;
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
			sessionState = initialSessionStateBuilder.apply(this);
			// This is the main loop!
			while (sessionState != null) {
				BaseCTOS packet = PacketReader.readPacket(config, socketInput);
				if (packet == null)
					break;
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
		}
	}
}
