/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.csc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import natsue.data.babel.PacketReader;
import natsue.data.babel.PacketWriter;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSHandshake;
import natsue.data.babel.ctos.CTOSMessage;
import natsue.data.babel.ctos.CTOSUnknown;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.csi.IClientServerInterface;
import natsue.server.csi.IConnectedClient;

/**
 * Thread for a given client.
 */
public class SocketThread extends Thread implements ILogSource, IConnectedClient {
	public final Socket socket;
	private InputStream socketInput;
	private OutputStream socketOutput;
	private final Object sendPacketLock = new Object();
	public final IClientServerInterface serverHub;
	public final ILogProvider log;
	public final PacketReader packetReader;
	public long myUIN;

	public SocketThread(Socket skt, IClientServerInterface csi, ILogProvider ilp, PacketReader pr) {
		socket = skt;
		serverHub = csi;
		log = ilp;
		packetReader = pr;
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
	public void run() {
		try {
			// Set initial settings
			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			socket.setKeepAlive(true);
			// Get handshake packet
			BaseCTOS packet = packetReader.readPacket(socketInput);
			if (packet != null) {
				if (packet instanceof CTOSHandshake) {
					if (handleHandshakePacket((CTOSHandshake) packet)) {
						while (true) {
							packet = packetReader.readPacket(socketInput);
							if (packet == null)
								break;
							handleGeneralPacket(packet);
						}
					}
				} else {
					sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_INTERNAL_ERROR, 0L, 0L));
				}
			}
			// nevermind then
		} catch (Exception ex) {
			logTo(log, ex);
		} finally {
			if (myUIN != 0)
				serverHub.logout(myUIN, this);
			try {
				socket.close();
			} catch (Exception ex2) {
				// Deliberately ignored - we're closing the socket.
			}
		}
	}

	private boolean handleHandshakePacket(CTOSHandshake handshake) throws IOException {
		CTOSHandshake theHandshake = (CTOSHandshake) handshake;
		if (theHandshake.username.equals("coral")) {
			sendPacket(PacketWriter.writeHandshakeResponse(Integer.valueOf(theHandshake.password), 0L, 0L));
			return false;
		} else if (theHandshake.username.equals("username") && theHandshake.password.equals("password")) {
			sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_OK, 7L, 7L));
			return true;
		} else {
			sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_INVALID_USER, 0L, 0L));
			return false;
		}
	}

	private void handleGeneralPacket(BaseCTOS packet) throws IOException {
		if (packet instanceof CTOSMessage) {
			// uuuuhh discard for now
		} else {
			// handle any packet that has no specific handler
			byte[] dummy = packet.makeDummy();
			if (dummy != null)
				sendPacket(dummy);
		}
	}
}
