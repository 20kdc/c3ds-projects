/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

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
	public final ArrayList<Long> randomPool = new ArrayList<>();
	public final Random randomGen = new Random();

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
	public long getRandomOnlineNonSystemUIN() {
		synchronized (this) {
			int size = randomPool.size();
			if (size == 0)
				return 0;
			int idx = randomGen.nextInt(size);
			return randomPool.get(idx);
		}
	}

	@Override
	public boolean forceRouteMessage(long destinationUIN, PackedMessage message) throws IOException {
		IHubClient ihc;
		synchronized (this) {
			ihc = connectedClients.get(destinationUIN);
		}
		if (ihc == null)
			return false;
		ihc.incomingMessage(message);
		return true;
	}

	@Override
	public void spoolMessage(long destinationUIN, PackedMessage message) {
		boolean success = false;
		try {
			success = forceRouteMessage(destinationUIN, message);
		} catch (Exception ex) {
			// expected, so don't bother logging
		}
		if (!success) {
			// Message stops with us. But only spool to users in the database.
			if (UINUtils.hid(destinationUIN) == UINUtils.HID_USER) {
				int uid = UINUtils.uid(destinationUIN);
				if (database.getUserByUID(uid) != null)
					database.spoolMessage(uid, message.toByteArray());
			}
		}
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
				if (!cc.isSystem())
					randomPool.add(uin);
			}
		}
		for (IHubClient ihc : wwrNotify)
			ihc.wwrNotify(true, userData);
		if (UINUtils.hid(uin) == UINUtils.HID_USER) {
			int uid = UINUtils.uid(uin);
			// This is presumably a user in the database, dump all spool contents
			while (true) {
				byte[] pm = database.popFirstSpooledMessage(uid);
				if (pm != null) {
					try {
						cc.incomingMessage(new PackedMessage(pm));
					} catch (Exception ex) {
						// Something went wrong, re-spool the message
						database.spoolMessage(uid, pm);
						break;
					}
				} else {
					break;
				}
			}
		}
		return true;
	}

	@Override
	public void clientLogout(IHubClient cc) {
		BabelShortUserData userData = cc.getUserData();
		Long uin = userData.uin;
		LinkedList<IHubClient> wwrNotify = new LinkedList<>();
		synchronized (this) {
			randomPool.remove(uin);
			if (connectedClients.get(uin) == cc) {
				connectedClients.remove(uin);
			}
		}
		for (IHubClient ihc : wwrNotify)
			ihc.wwrNotify(false, userData);
	}

	@Override
	public void clientGiveMessage(IHubClient cc, long destinationUIN, PackedMessage message) {
		if (message.senderUIN != cc.getUserData().uin)
			return;
		spoolMessage(destinationUIN, message);
	}
}
