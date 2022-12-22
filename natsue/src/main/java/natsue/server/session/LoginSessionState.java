/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.session;

import java.io.IOException;

import natsue.config.Config;
import natsue.data.babel.PacketWriter;
import natsue.data.babel.ctos.BaseCTOS;
import natsue.data.babel.ctos.CTOSHandshake;
import natsue.data.hli.StandardMessages;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.hubapi.IHubClientAPI;
import natsue.server.hubapi.IHubLoginAPI;
import natsue.server.hubapi.IHubLoginAPI.ILoginReceiver;
import natsue.server.hubapi.IHubLoginAPI.LoginResult.AccountFrozen;
import natsue.server.userdata.INatsueUserData;

/**
 * This session state is to grab the initial handshake packet.
 */
public class LoginSessionState extends BaseSessionState implements ILogSource {
	public final IHubLoginAPI hub;
	public final Config config;

	public LoginSessionState(Config cfg, ISessionClient c, IHubLoginAPI h) {
		super(c);
		hub = h;
		config = cfg;
	}

	@Override
	public ILogProvider getLogParent() {
		return client;
	}

	@Override
	public void handlePacket(BaseCTOS packet) throws IOException {
		if (packet instanceof CTOSHandshake) {
			handleHandshakePacket((CTOSHandshake) packet);
		} else {
			// nope
			client.sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_UNKNOWN, 0L, 0L));
			client.setSessionState(null);
		}
	}

	public void handleHandshakePacket(CTOSHandshake handshake) throws IOException {
		// -- attempt normal login --
		IHubLoginAPI.LoginResult res = hub.loginUser(handshake.username, handshake.password, new ILoginReceiver<MainSessionState>() {
			@Override
			public MainSessionState receive(INatsueUserData.Root userData, IHubClientAPI clientAPI) {
				return new MainSessionState(config, client, clientAPI, userData);
			}
			@Override
			public void confirm(MainSessionState result) {
				client.setSessionState(result);
				try {
					client.sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_OK, result.hub.getServerUIN(), result.userData.getUIN()));
				} catch (Exception ex) {
					if (client.logFailedAuth())
						log(ex);
				}
			}
		});
		if (res == IHubLoginAPI.LoginResult.FAILED_AUTH) {
			client.sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_INVALID_USER, 0L, 0L));
			client.setSessionState(null);
		} else if (res instanceof IHubLoginAPI.LoginResult.FailedConflict) {
			client.sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_ALREADY_LOGGED_IN, 0L, 0L));
			client.setSessionState(null);
		} else if (res instanceof IHubLoginAPI.LoginResult.AccountFrozen) {
			AccountFrozen af = (AccountFrozen) res;
			// Alright, this gets complicated
			long uin = af.who.uin;
			client.sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_OK, af.serverUIN, uin));
			client.sendPacket(PacketWriter.writeMessage(StandardMessages.systemMessage(uin, config.accountFrozenText.getValue()).toByteArray()));
			try {
				// We don't want the client to consider this a complete login.
				// But we need enough time for the client to receive our message without the error coming up.
				// Otherwise the system message will be lost.
				Thread.sleep(10000);
			} catch (Exception ex) {
				// done!
			}
			client.setSessionState(null);
		} else if (res != IHubLoginAPI.LoginResult.SUCCESS) {
			client.sendPacket(PacketWriter.writeHandshakeResponse(PacketWriter.HANDSHAKE_RESPONSE_UNKNOWN, 0L, 0L));
			client.setSessionState(null);
		}
	}

	@Override
	public void logout() {
		// nothing to do here
	}
}
