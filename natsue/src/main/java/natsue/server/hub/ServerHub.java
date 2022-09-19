/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import natsue.config.Config;
import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;
import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.PWHash;
import natsue.server.database.UsernameVerifier;
import natsue.server.database.INatsueDatabase.UserInfo;
import natsue.server.firewall.IFirewall;
import natsue.server.hubapi.IHubClient;
import natsue.server.hubapi.IHubClientAPI;
import natsue.server.hubapi.IHubPrivilegedClientAPI;

/**
 * Class that contains everything important to everything ever.
 */
public class ServerHub implements IHubPrivilegedClientAPI, ILogSource {
	public final Config config;
	private final ILogProvider logParent;
	public final INatsueDatabase database;
	/**
	 * The firewall. This should be set before the server receives any clients, on pain of malfunction.
	 */
	private IFirewall firewall;

	public final HashSet<IWWRListener> wwrListeners = new HashSet<>();
	public final HashMap<Long, IHubClient> connectedClients = new HashMap<>();
	public final HashMap<String, IHubClient> connectedClientsByNickname = new HashMap<>();
	public final ArrayList<Long> randomPool = new ArrayList<>();
	public final Random randomGen = new Random();

	public ServerHub(Config cfg, ILogProvider logProvider, INatsueDatabase db) {
		config = cfg;
		logParent = logProvider;
		database = db;
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	public void setFirewall(IFirewall fw) {
		firewall = fw;
		wwrListeners.add(fw);
	}

	@Override
	public LinkedList<BabelShortUserData> listAllNonSystemUsersOnlineYesIMeanAllOfThem() {
		LinkedList<BabelShortUserData> ll = new LinkedList<>();
		synchronized (this) {
			for (IHubClient client : connectedClients.values())
				if (!client.isSystem())
					ll.add(client.getUserData());
		}
		return ll;
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
	public BabelShortUserData getShortUserDataByNickname(String name) {
		name = UsernameVerifier.foldNickname(name);
		IHubClient ihc;
		synchronized (this) {
			ihc = connectedClientsByNickname.get(name);
		}
		if (ihc != null)
			return ihc.getUserData();
		// Ok, now check with database
		if (!UsernameVerifier.verifyNickname(name))
			return null;
		UserInfo ui = database.getUserByFoldedNickname(name);
		if (ui != null)
			return ui.convertToBabel();
		return null;
	}

	@Override
	public boolean isUINOnline(long uin) {
		synchronized (this) {
			return connectedClients.containsKey(uin);
		}
	}

	@Override
	public BabelShortUserData usernameAndPasswordToShortUserData(String username, String password, boolean allowedToRegister) {
		String usernameFolded = UsernameVerifier.foldUsername(username);
		if (!UsernameVerifier.verifyUsername(usernameFolded))
			return null;
		UserInfo ui = database.getUserByFoldedUsername(usernameFolded);
		if (allowedToRegister && ui == null && config.allowRegistration.getValue()) {
			while (ui == null) {
				int uid;
				synchronized (this) {
					uid = randomGen.nextInt();
				}
				// don't register with 0 or negative numbers
				// negative numbers will probably fry the Warp inbox system!!!
				if (uid <= 0)
					continue;
				boolean success = database.tryCreateUser(uid, usernameFolded, username, UsernameVerifier.foldNickname(username), PWHash.hash(uid, password));
				if (success)
					log("Registered user: " + username + " as UID " + uid);
				// It's possible that a username collision occurred during the registration process.
				// In that event, we obviously should be seeing a username here.
				ui = database.getUserByFoldedUsername(usernameFolded);
			}
		}
		if (ui == null)
			return null;
		// this isn't how this is supposed to work but let's ignore that right now
		if (ui.passwordHash != null)
			if (PWHash.verify(ui.uid, ui.passwordHash, password))
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

	private void spoolMessage(long destinationUIN, PackedMessage message) {
		if (UINUtils.hid(destinationUIN) == UINUtils.HID_USER) {
			int uid = UINUtils.uid(destinationUIN);
			if (database.getUserByUID(uid) != null)
				database.spoolMessage(uid, message.toByteArray());
		}
	}

	@Override
	public void sendMessage(long destinationUIN, PackedMessage message, boolean temp) {
		IHubClient ihc;
		if (temp) {
			synchronized (this) {
				ihc = connectedClients.get(destinationUIN);
			}
			if (ihc != null)
				ihc.incomingMessage(message, null);
		} else {
			synchronized (this) {
				ihc = connectedClients.get(destinationUIN);
				if (ihc == null) {
					// They're not online, so do this here - otherwise they *could* go online while we're spooling the message (BAD!)
					// If they go offline while we're SENDING the message, that's caught by the reject machinery
					spoolMessage(destinationUIN, message);
					return;
				}
			}
			if (ihc != null) {
				ihc.incomingMessage(message, () -> {
					spoolMessage(destinationUIN, message);
				});
			}
		}
	}

	/**
	 * Must run in synchronized block, or else events will come too early.
	 */
	private LinkedList<IWWRListener> earlyClientLoginInSync(IHubClient cc) {
		BabelShortUserData userData = cc.getUserData();
		Long uin = userData.uin;
		LinkedList<IWWRListener> wwrNotify;
		if (connectedClients.containsKey(uin)) {
			return null;
		} else {
			connectedClients.put(uin, cc);
			String foldedNick = UsernameVerifier.foldNickname(userData.nickName);
			connectedClientsByNickname.put(foldedNick, cc);
			if (!cc.isSystem())
				randomPool.add(uin);
			wwrNotify = new LinkedList<IWWRListener>(wwrListeners);
			wwrListeners.add(cc);
		}
		return wwrNotify;
	}

	private void lateClientLogin(IHubClient cc, LinkedList<IWWRListener> wwrNotify) {
		BabelShortUserData userData = cc.getUserData();
		for (IWWRListener ihc : wwrNotify)
			ihc.wwrNotify(true, cc.getUserData());
		if (UINUtils.hid(userData.uin) == UINUtils.HID_USER) {
			int uid = UINUtils.uid(userData.uin);
			// This is presumably a user in the database, dump all spool contents
			while (true) {
				byte[] pm = database.popFirstSpooledMessage(uid);
				if (pm != null) {
					cc.incomingMessage(new PackedMessage(pm), () -> {
						database.spoolMessage(uid, pm);
					});
				} else {
					break;
				}
			}
		}
	}

	@Override
	public boolean clientLogin(IHubClient client, Runnable onConfirm) {
		LinkedList<IWWRListener> wwrNotify;
		synchronized (this) {
			wwrNotify = earlyClientLoginInSync(client);
			if (wwrNotify == null)
				return false;
			onConfirm.run();
		}
		lateClientLogin(client, wwrNotify);
		return true;
	}

	@Override
	public <X extends IHubClient> LoginResult loginUser(String username, String password, ILoginReceiver<X> makeClient) {
		BabelShortUserData userData = usernameAndPasswordToShortUserData(username, password, true);
		X client = makeClient.receive(userData, this);
		LinkedList<IWWRListener> wwrNotify;
		synchronized (this) {
			wwrNotify = earlyClientLoginInSync(client);
			if (wwrNotify == null)
				return LoginResult.FailedConflict;
			wwrNotify = new LinkedList<IWWRListener>(wwrListeners);
			makeClient.confirm(client);
		}
		lateClientLogin(client, wwrNotify);
		return LoginResult.Success;
	}

	@Override
	public void clientLogout(IHubClient cc) {
		BabelShortUserData userData = cc.getUserData();
		Long uin = userData.uin;
		LinkedList<IWWRListener> wwrNotify;
		synchronized (this) {
			randomPool.remove(uin);
			if (connectedClients.get(uin) == cc) {
				connectedClients.remove(uin);
				String foldedNick = UsernameVerifier.foldNickname(userData.nickName);
				connectedClientsByNickname.remove(foldedNick);
			}
			wwrNotify = new LinkedList<IWWRListener>(wwrListeners);
			wwrListeners.remove(cc);
		}
		for (IWWRListener ihc : wwrNotify)
			ihc.wwrNotify(false, userData);
	}

	@Override
	public void clientGiveMessage(IHubClient cc, long destinationUIN, PackedMessage message) {
		firewall.handleMessage(cc.getUserData(), destinationUIN, message);
	}
}
