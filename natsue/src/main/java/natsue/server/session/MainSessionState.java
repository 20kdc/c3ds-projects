/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import cdsp.common.data.IOUtils;
import cdsp.common.util.TOTP;
import natsue.config.Config;
import natsue.data.babel.BabelClientVersion;
import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.PacketWriter;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSFeedHistory;
import natsue.data.babel.ctos.CTOSFetchRandomUser;
import natsue.data.babel.ctos.CTOSGetClientInfo;
import natsue.data.babel.ctos.CTOSGetConnectionDetail;
import natsue.data.babel.ctos.CTOSMessage;
import natsue.data.babel.ctos.CTOSVirtualCircuit;
import natsue.data.babel.ctos.CTOSVirtualConnect;
import natsue.data.babel.ctos.CTOSWWRModify;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.hli.StandardMessages;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.hubapi.IHubClient;
import natsue.server.hubapi.IHubClientAPI;
import natsue.server.userdata.INatsueUserData;

/**
 * This session state is used while connected to the hub.
 */
public class MainSessionState extends BaseSessionState implements IHubClient, ILogSource {
	public final INatsueUserData.LongTermPrivileged userData;
	public final IHubClientAPI hub;
	public final PingManager pingManager;
	public final Config config;
	public boolean notReallyOnline;
	public final BabelClientVersion myClientVersion;
	public final byte[] twoFASecret;
	private volatile boolean has2FAAuthed;

	public MainSessionState(Config cfg, ISessionClient c, BabelClientVersion myClientVersion, byte[] twoFASecret, IHubClientAPI h, INatsueUserData.LongTermPrivileged uin) {
		super(c);
		this.myClientVersion = myClientVersion;
		this.twoFASecret = twoFASecret;
		if (twoFASecret == null)
			has2FAAuthed = true;
		config = cfg;
		pingManager = new PingManager(c);
		userData = uin;
		hub = h;
	}

	@Override
	public ILogProvider getLogParent() {
		return client;
	}

	@Override
	public INatsueUserData.Root getUserData() {
		return userData;
	}

	@Override
	public BabelClientVersion getClientVersion() {
		return myClientVersion;
	}

	@Override
	public boolean has2FAAuthed() {
		return has2FAAuthed;
	}

	@Override
	public boolean try2FAAuth(int code) {
		if (twoFASecret != null) {
			if (TOTP.verify(twoFASecret, code)) {
				has2FAAuthed = true;
				return true;
			}
		}
		return false;
	}

	@Override
	public ISessionClient acquireSessionClientForResearchCommands() {
		return client;
	}

	@Override
	public void handlePacket(BaseCTOS packet) throws IOException {
		// check for ping-related stuff
		if (pingManager.handleResponse(packet))
			return;

		if (packet instanceof CTOSGetConnectionDetail) {
			CTOSGetConnectionDetail pkt = (CTOSGetConnectionDetail) packet; 
			// well, are they connected?
			boolean result = hub.getConnectionByUIN(pkt.targetUIN) != null;
			if (result) {
				client.sendPacket(pkt.makeOkResponse());
			} else {
				dummyResponse(packet);
			}
		} else if (packet instanceof CTOSGetClientInfo) {
			CTOSGetClientInfo pkt = (CTOSGetClientInfo) packet;
			INatsueUserData bsud = hub.getUserDataByUIN(pkt.targetUIN);
			client.sendPacket(pkt.makeResponse(bsud != null ? bsud.getBabelUserData().packed : null));
		} else if (packet instanceof CTOSWWRModify) {
			CTOSWWRModify pkt = (CTOSWWRModify) packet;
			// So to avoid someone flooding the system with a really big WWR, I've decided to simply pretend the WWR exists.
			// And actually tell clients about EVERYBODY.
			// But we should also give proper WWR indications when asked.
			if (pkt.add) {
				INatsueUserData bsud = hub.getUserDataByUIN(pkt.targetUIN);
				if (bsud != null) {
					try {
						// isUINOnline now updates before WWR handlers run.
						// Any upcoming WWR handlers are delayed by the lock.
						// So the state gotten either is accurate or will be updated once that lock is released.
						synchronized (this) {
							boolean online = hub.getConnectionByUIN(pkt.targetUIN) != null;
							client.sendPacket(PacketWriter.writeUserLine(online, bsud.getBabelUserData().packed));
						}
					} catch (IOException e) {
						log(e);
					}
				}
			}
		} else if (packet instanceof CTOSFetchRandomUser) {
			CTOSFetchRandomUser pkt = (CTOSFetchRandomUser) packet;
			client.sendPacket(pkt.makeResponse(hub.getRandomOnlineNonSystemUIN(config.excludeSelfRUSO.getValue() ? getUIN() : 0)));
		} else if (packet instanceof CTOSMessage) {
			CTOSMessage pkt = (CTOSMessage) packet;
			try {
				PackedMessage pm = PackedMessage.read(pkt.messageData, config.messages);
				hub.clientGiveMessage(this, pkt.targetUIN, pm);
			} catch (Exception ex) {
				log(ex);
			}
		} else if (packet instanceof CTOSFeedHistory) {
			try {
				ByteBuffer bb = IOUtils.wrapLE(((CTOSFeedHistory) packet).data);
				CreatureHistoryBlob chb = new CreatureHistoryBlob(bb, config.messages.maxCreatureHistoryEvents.getValue());
				hub.clientSendHistory(this, chb);
			} catch (Exception ex) {
				log(ex);
			}
			dummyResponse(packet);
		} else if (packet instanceof CTOSVirtualConnect) {
			// Crash 'prevention'.
			// ...you all get to share this one VSN
			client.sendPacket(PacketWriter.writeVirtualConnectResponse(((CTOSVirtualConnect) packet).sourceVSN, (short) 1));
			// Let's not silently fail
			PackedMessage pm = StandardMessages.systemMessage(getUIN(),
					"Natsue Automated Message\n" +
					"~~~~~~~~~~~~~~~~~~~~~~~~\n" +
					"You have attempted to open a 'virtual circuit connection'.\n" +
					"This probably means you ran NET: WRIT on the original engine.\n" +
					"\n" +
					"Simply put: This feature is broken.\n" +
					"Signs so far indicate it likely did not work even on original server software.\n" +
					"It has no negative effect on Natsue, but is very bad for your client.\n" +
					"\n" +
					"TLDR:\n" +
					"YOU'VE BEEN SAVED FROM CRASHING, BUT CLOSE THE GAME SAFELY AS SOON AS POSSIBLE.\n" +
					"\n" +
					"Do ask me about this! - 20kdc");
			client.sendPacket(PacketWriter.writeMessage(pm.toByteArray(config.messages.compressPRAYChunks.getValue())));
		} else if (packet instanceof CTOSVirtualCircuit) {
			// Do nothing.
			//CTOSVirtualCircuit vc = (CTOSVirtualCircuit) packet;
			/*
			System.out.println("VIRTUAL CIRCUIT DATA");
			for (byte b : vc.messageData) {
				System.out.println("\t" + b);
			}
			*/
			//client.sendPacket(PacketWriter.writeVirtualCircuitData(vc.targetUIN, vc.targetVSN, getUIN(), vc.sourceVSN, vc.messageData));
		} else {
			dummyResponse(packet);
		}
	}
	public void dummyResponse(BaseCTOS packet) throws IOException {
		byte[] dummy = packet.makeDummy();
		if (dummy != null)
			client.sendPacket(dummy);
	}

	@Override
	public void logout() {
		hub.clientLogout(this);
		pingManager.logout();
	}

	@Override
	public boolean forceDisconnect(boolean sync) {
		client.forceDisconnect(sync);
		return true;
	}

	@Override
	public synchronized void wwrNotify(boolean online, INatsueUserData userData) {
		// no actual WWR but do give notifications
		try {
			client.sendPacket(PacketWriter.writeUserLine(online, userData.getBabelUserData().packed));
		} catch (IOException e) {
			log(e);
		}
	}

	@Override
	public void incomingMessage(PackedMessage message, Runnable reject, boolean compressIfAllowed) {
		incomingMessageByteArrayFastPath(message.toByteArray(config.messages.compressPRAYChunks.getValue() && compressIfAllowed), reject);
	}

	@Override
	public boolean incomingMessageByteArrayFastPath(byte[] message, Runnable reject) {
		// First of all, send the message
		try {
			client.sendPacket(PacketWriter.writeMessage(message));
		} catch (Exception ex) {
			log(ex);
			if (reject != null)
				reject.run();
			return true;
		}
		// Now setup tracking for if that fails
		if (reject == null)
			return true;
		final AtomicBoolean hasRejected = new AtomicBoolean();
		byte[] pingPacket = pingManager.addPing((status) -> {
			if (hasRejected.getAndSet(true))
				return;
			if (status == 0)
				reject.run();
		});
		if (pingPacket == null) {
			if (!hasRejected.getAndSet(true))
				reject.run();
			return true;
		}
		try {
			client.sendPacket(pingPacket);
		} catch (Exception ex) {
			log(ex);
			if (!hasRejected.getAndSet(true))
				reject.run();
		}
		return true;
	}

	@Override
	public boolean isNotReallyOnline() {
		return notReallyOnline;
	}

	@Override
	public void markNotReallyOnline() {
		notReallyOnline = true;
	}

}
