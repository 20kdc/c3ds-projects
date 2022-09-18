/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import natsue.config.IConfigProvider;
import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;
import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.INatsueDatabase.UserInfo;

/**
 * Class that contains everything important to everything ever.
 */
public class ServerHub implements IHub, ILogSource {
	public final IConfigProvider config;
	public final ILogProvider log;
	public final INatsueDatabase database;

	public final HashMap<Long, IHubClient> connectedClients = new HashMap<>();

	public ServerHub(IConfigProvider cfg, ILogProvider logProvider, INatsueDatabase db) {
		config = cfg;
		log = logProvider;
		database = db;
		// login the system user
		clientLogin(new SystemUserHubClient(this), () -> {});
	}

	@Override
	public BabelShortUserData getShortUserDataByUIN(long uin) {
		// Try a direct database lookup so that the system user doesn't asplode
		IHubClient ihc;
		synchronized (this) {
			ihc = connectedClients.get(uin);
		}
		if (ihc != null)
			return ihc.getUserData();
		// No? Oh well, then
		if (UINUtils.hid(uin) == UINUtils.HID_USER) {
			UserInfo ui = database.getUserByUID(UINUtils.uid(uin));
			if (ui != null)
				return ui.convertToBabel();
		}
		return null;
	}

	@Override
	public boolean isUINOnline(long uin) {
		synchronized (this) {
			return connectedClients.containsKey(uin);
		}
	}

	@Override
	public BabelShortUserData usernameAndPasswordToShortUserData(String username, String password) {
		UserInfo ui = database.getUserByUsername(username);
		if (ui == null)
			return null;
		// this isn't how this is supposed to work but let's ignore that right now
		if (ui.passwordHash != null)
			if (ui.passwordHash.equals(password))
				return ui.convertToBabel();
		return null;
	}

	@Override
	public long getServerUIN() {
		return UINUtils.SERVER_UIN;
	}

	@Override
	public boolean forceRouteMessage(long destinationUID, PackedMessage message) throws IOException {
		IHubClient ihc;
		synchronized (this) {
			ihc = connectedClients.get(destinationUID);
		}
		if (ihc == null)
			return false;
		ihc.incomingMessage(message);
		return true;
	}

	@Override
	public boolean clientLogin(IHubClient cc, Runnable onConfirm) {
		BabelShortUserData userData = cc.getUserData();
		Long uin = userData.uin;
		LinkedList<IHubClient> wwrNotify = new LinkedList<>();
		synchronized (this) {
			if (connectedClients.containsKey(uin)) {
				return false;
			} else {
				wwrNotify.addAll(connectedClients.values());
				connectedClients.put(uin, cc);
				onConfirm.run();
			}
		}
		for (IHubClient ihc : wwrNotify)
			ihc.wwrNotify(true, userData);
		return true;
	}

	@Override
	public void clientLogout(IHubClient cc) {
		BabelShortUserData userData = cc.getUserData();
		Long uin = userData.uin;
		LinkedList<IHubClient> wwrNotify = new LinkedList<>();
		synchronized (this) {
			if (connectedClients.get(uin) == cc) {
				connectedClients.remove(uin);
			}
		}
		for (IHubClient ihc : wwrNotify)
			ihc.wwrNotify(false, userData);
	}

	@Override
	public void clientGiveMessage(IHubClient cc, long destinationUID, PackedMessage message) {
		if (message.senderUIN != cc.getUserData().uin)
			return;
		try {
			forceRouteMessage(destinationUID, message);
		} catch (Exception ex) {
			// :( message lost
			logTo(log, ex);
		}
	}
}
