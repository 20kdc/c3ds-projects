/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.io.IOException;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;
import natsue.data.babel.PacketWriter;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSGetClientInfo;
import natsue.data.babel.ctos.CTOSGetConnectionDetail;
import natsue.data.babel.ctos.CTOSWWRModify;
import natsue.log.ILogSource;
import natsue.server.hub.IHub;
import natsue.server.hub.IHubClient;

/**
 * This session state is used while connected to the hub.
 */
public class MainSessionState extends BaseSessionState implements IHubClient, ILogSource {
	public final BabelShortUserData userData;
	public final IHub hub;

	public MainSessionState(ISessionClient c, IHub h, BabelShortUserData uin) {
		super(c);
		userData = uin;
		hub = h;
	}

	@Override
	public BabelShortUserData getUserData() {
		return userData;
	}

	@Override
	public void handlePacket(BaseCTOS packet) throws IOException {
		if (packet instanceof CTOSGetConnectionDetail) {
			CTOSGetConnectionDetail pkt = (CTOSGetConnectionDetail) packet; 
			// well, are they connected?
			boolean result = hub.isUINOnline(pkt.targetUIN);
			if (result) {
				client.sendPacket(pkt.makeOkResponse());
			} else {
				dummyResponse(packet);
			}
		} else if (packet instanceof CTOSGetClientInfo) {
			CTOSGetClientInfo pkt = (CTOSGetClientInfo) packet;
			BabelShortUserData bsud = hub.getShortUserDataByUIN(pkt.targetUIN);
			client.sendPacket(pkt.makeResponse(bsud != null ? bsud.packed : null));
		} else if (packet instanceof CTOSWWRModify) {
			CTOSWWRModify pkt = (CTOSWWRModify) packet;
			// So to avoid someone flooding the system with a really big WWR, I've decided to simply pretend the WWR exists.
			// And actually tell clients about EVERYBODY.
			// But we should also give proper WWR indications when asked.
			if (pkt.add) {
				BabelShortUserData bsud = hub.getShortUserDataByUIN(pkt.targetUIN);
				if (bsud != null)
					wwrNotify(hub.isUINOnline(pkt.targetUIN), bsud);
			}
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
	}

	@Override
	public void wwrNotify(boolean online, BabelShortUserData userData) {
		// no actual WWR but do give notifications
		try {
			client.sendPacket(PacketWriter.writeUserLine(online, userData.packed));
		} catch (IOException e) {
			logTo(client, e);
		}
	}

	@Override
	public void incomingMessage(PackedMessage message) throws IOException {
		client.sendPacket(PacketWriter.writeMessage(message.toByteArray()));
	}
}
