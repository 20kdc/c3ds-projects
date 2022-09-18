/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import natsue.ILogProvider;
import natsue.ILogSource;
import natsue.data.babel.ctos.BaseCTOS;

/**
 * Thread for a given client.
 */
public class SocketThread extends Thread implements ILogSource {
	public final Socket socket;
	private InputStream socketInput;
	private OutputStream socketOutput;
	public final ServerHub serverHub;
	public final ILogProvider log;

	public SocketThread(Socket skt, ServerHub sh) {
		socket = skt;
		serverHub = sh;
		log = sh.log;
	}

	@Override
	public void run() {
		try {
			// Set initial settings
			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			socket.setKeepAlive(true);
			// Get handshake packet
			BaseCTOS packet = serverHub.packetReader.readPacket(socketInput);
			if (packet != null)
				actuallyRunWithHandshakePacket(packet);
			// nevermind then
		} catch (Exception ex) {
			logTo(log, ex);
		}
		try {
			socket.close();
		} catch (Exception ex2) {
			// Deliberately ignored - we're closing the socket.
		}
	}

	public void actuallyRunWithHandshakePacket(BaseCTOS handshake) {
		logTo(log, "no actual implementation here");
	}
}
