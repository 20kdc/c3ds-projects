/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import natsue.config.Config;
import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.UINUtils;
import natsue.data.babel.CreatureHistoryBlob.LifeEvent;
import natsue.data.babel.pm.PackedMessage;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.names.CreatureDataVerifier;
import natsue.server.cryo.CryoFrontend;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.NatsueDBUserInfo;
import natsue.server.firewall.IFWModule;
import natsue.server.firewall.IRejector;
import natsue.server.hubapi.IHubClient;
import natsue.server.hubapi.IHubPrivilegedClientAPI;
import natsue.server.packet.QuotaManager;
import natsue.server.session.ISessionClient;
import natsue.server.session.MainSessionState;
import natsue.server.system.SystemCommands;
import natsue.server.userdata.IHubUserDataCacheBetweenCacheAndHub;
import natsue.server.userdata.IHubUserDataCachePrivileged;
import natsue.server.userdata.INatsueUserData;
import natsue.server.userdata.INatsueUserData.LongTerm;

/**
 * Class that contains everything important to everything ever.
 */
public class ServerHub implements IHubPrivilegedClientAPI, ILogSource {
	public final Config config;
	private final ILogProvider logParent;
	private final QuotaManager quotaManager;
	public final INatsueDatabase database;

	/**
	 * The firewall. This should be set before the server receives any clients, on pain of malfunction.
	 */
	private IFWModule[] firewall;

	/**
	 * The rejector, similar to firewall.
	 * Only use this via rejectMessage because we may want to log these.
	 */
	private IRejector rejector;

	/**
	 * Cache of active users.
	 */
	private final IHubUserDataCacheBetweenCacheAndHub userDataCache;

	private final HubUserRegister users;

	private final Random randomGen = new Random();

	/**
	 * We only have this for debugging
	 */
	private final CryoFrontend cryo;

	public ServerHub(Config cfg, QuotaManager qm, ILogProvider logProvider, INatsueDatabase db, CryoFrontend c) {
		config = cfg;
		logParent = logProvider;
		quotaManager = qm;
		cryo = c;
		database = db;
		userDataCache = new HubUserDataCache(database, cfg.accounts, logProvider);
		users = new HubUserRegister(userDataCache);
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	@Override
	public CryoFrontend getCryoFE() {
		return cryo;
	}

	public void setFirewall(IFWModule[] fw, IRejector ir) {
		firewall = fw;
		rejector = ir;
		// This is assumed to be occurring before the server goes multi-threaded.
		for (IFWModule fm : fw)
			users.wwrListeners.add(fm);
	}

	@Override
	public LinkedList<INatsueUserData> listAllNonSystemUsersOnlineYesIMeanAllOfThem() {
		LinkedList<INatsueUserData> ll = new LinkedList<>();
		synchronized (this) {
			for (IHubClient client : users.connectedClients.values())
				if (!client.isSystem())
					ll.add(client.getUserData());
		}
		return ll;
	}

	@Override
	public boolean isUINOnline(long uin) {
		synchronized (this) {
			IHubClient ihc = users.connectedClients.get(uin);
			if (ihc != null)
				return !ihc.isNotReallyOnline();
			return false;
		}
	}

	@Override
	public long getServerUIN() {
		return UINUtils.SERVER_UIN;
	}

	@Override
	public boolean isUINAdmin(long targetUIN) {
		INatsueUserData userData = getUserDataByUIN(targetUIN);
		if (userData != null)
			return userData.isAdmin();
		return false;
	}

	@Override
	public long getRandomOnlineNonSystemUIN(long whoIsnt) {
		long result = 0;
		Long wiObj = whoIsnt == 0 ? null : whoIsnt;
		synchronized (this) {
			// sanity check
			LinkedList<Long> notSupposedToBeThere = new LinkedList<>(users.randomPool);
			notSupposedToBeThere.removeIf((l) -> users.connectedClients.containsKey(l));
			if (notSupposedToBeThere.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("INCONSISTENCY IN RANDOM POOL:");
				for (Long l : notSupposedToBeThere) {
					sb.append(' ');
					sb.append(UINUtils.toString(l));
				}
				log(sb.toString());
				users.randomPool.removeAll(notSupposedToBeThere);
			}
			// It's easier to remove and re-add the object than to rebuild the pool, even if it is dumb.
			boolean needsReadd = false;
			if (wiObj != null)
				needsReadd = users.randomPool.remove(wiObj);
			int size = users.randomPool.size();
			if (size != 0) {
				int idx = randomGen.nextInt(size);
				result = users.randomPool.get(idx);
			}
			if (needsReadd)
				users.randomPool.add(wiObj);
		}
		return result;
	}

	@Override
	public void rejectMessage(long destinationUIN, PackedMessage message, String reason) {
		log("Rejected message from " + UINUtils.toString(message.senderUIN) + " b/c: " + reason);
		try {
			rejector.rejectMessage(destinationUIN, message, reason);
		} catch (Exception ex) {
			log(ex);
		}
	}

	/**
	 * WARNING: Only call if you're absolutely sure the person isn't presently online!
	 */
	private void spoolMessage(long destinationUIN, PackedMessage message, int trueSenderUID, boolean fromRejector) {
		NatsueDBUserInfo ui = database.getUserByUIN(destinationUIN);
		if (ui != null) {
			if (!database.spoolMessage(ui.uid, trueSenderUID, message.toByteArray(config.messages))) {
				if (!fromRejector) {
					// Spooling failed. There is almost nothing we can do, but there is one last thing we can try.
					rejectMessage(destinationUIN, message, "User " + UINUtils.toString(destinationUIN) + " spool failure");
				}
			}
		} else if (!fromRejector) {
			rejectMessage(destinationUIN, message, "User " + UINUtils.toString(destinationUIN) + " does not exist");
		}
	}

	@Override
	public void sendMessage(long destinationUIN, PackedMessage message, MsgSendType type, long causeUIN) {
		IHubClient ihc;
		if (type.failBehaviour.allowMessageLoss) {
			synchronized (this) {
				ihc = users.connectedClients.get(destinationUIN);
			}
			if (ihc != null) {
				ihc.incomingMessage(message, null);
			} else {
				sendMessageFailed(destinationUIN, message, type, causeUIN, "Target offline");
			}
		} else {
			// not temp, this message matters
			synchronized (this) {
				ihc = users.connectedClients.get(destinationUIN);
				if (ihc == null) {
					// They're not online, so do this here.
					// Otherwise they *could* go online while we're spooling the message (BAD!)
					// If they go offline while we're SENDING, that's caught by the reject machinery (see below)
					sendMessageFailed(destinationUIN, message, type, causeUIN, "Target offline");
				}
			}
			if (ihc != null) {
				ihc.incomingMessage(message, () -> {
					// If this gets run, we apparently couldn't send the message after all...
					sendMessageFailed(destinationUIN, message, type, causeUIN, "Target went offline during transmit");
				});
			}
		}
	}
	private void sendMessageFailed(long destinationUIN, PackedMessage message, MsgSendType type, long causeUIN, String reason) {
		switch (type.failBehaviour) {
		case Discard:
			return;
		case Spool:
			spoolMessage(destinationUIN, message, UINUtils.asDBUID(causeUIN), type.isReject);
			return;
		case Reject:
			rejectMessage(destinationUIN, message, reason);
			return;
		default:
			rejectMessage(destinationUIN, message, "FIXME: unknown fail behaviour");
		}
	}

	@Override
	public void forceDisconnectUIN(long uin, boolean sync) {
		IHubClient ihc;
		synchronized (this) {
			ihc = users.connectedClients.get(uin);
		}
		// We don't want the actual disconnect in the sync block.
		// This is because forceDisconnect is supposed to make absolutely sure the client is gone.
		// That implies a clientLogout needs to happen before it returns, and this may happen off-thread.
		if (ihc != null)
			ihc.forceDisconnect(sync); // X.X
	}

	private void wwrNotifyRun(boolean state, IHubClient cc, LinkedList<IWWRListener> wwrNotify) {
		for (IWWRListener ihc : wwrNotify) {
			try {
				ihc.wwrNotify(state, cc);
			} catch (Exception ex) {
				log(ex);
			}
		}
	}

	/**
	 * Note: For reliability reasons, this catches and logs exceptions internally.
	 * By this point, you see, the login has already been confirmed.
	 * Dumping the connection now would essentially corrupt caller state.
	 */
	private void lateClientLogin(IHubClient cc, LinkedList<IWWRListener> wwrNotify) {
		long uin = cc.getUIN();
		wwrNotifyRun(true, cc, wwrNotify);
		try {
			if (UINUtils.isRegularUser(uin)) {
				int uid = UINUtils.uid(uin);
				// This is presumably a user in the database, dump all spool contents
				int maxSpool = config.maxSpoolToReadOnConnect.getValue();
				for (int i = 0; i < maxSpool; i++) {
					byte[] pm = database.popFirstSpooledMessage(uid);
					if (pm != null) {
						PackedMessage pmi = PackedMessage.read(pm, config.messages);
						final int pmiSenderUID = UINUtils.asDBUID(pmi.senderUIN);
						cc.incomingMessage(pmi, () -> {
							// Note that BECAUSE THESE MESSAGES ARE ALREADY SPOOLED,
							//  it's okay to spool them again at this level.
							database.spoolMessage(uid, pmiSenderUID, pm);
						});
					} else {
						break;
					}
				}
			}
		} catch (Exception ex) {
			log(ex);
		}
	}

	@Override
	public boolean clientLogin(IHubClient client, Runnable onConfirm) {
		LinkedList<IWWRListener> wwrNotify;
		synchronized (this) {
			wwrNotify = users.earlyClientLoginInSync(client);
			if (wwrNotify == null)
				return false;
			try {
				onConfirm.run();
			} catch (Exception ex) {
				users.earlyClientLogoutInSync(client);
				throw ex;
			}
		}
		lateClientLogin(client, wwrNotify);
		return true;
	}

	private synchronized <X extends IHubClient> LinkedList<IWWRListener> earlyClientLoginWithReceiverStep(X client, ILoginReceiver<X> makeClient) {
		LinkedList<IWWRListener> wwrNotify = users.earlyClientLoginInSync(client);
		// wwrNotify being null here means a conflict happened.
		if (wwrNotify != null) {
			try {
				makeClient.confirm(client);
			} catch (Exception ex) {
				// An exception occurring here reads as login failure - clean up.
				users.earlyClientLogoutInSync(client);
				throw ex;
			}
		}
		return wwrNotify;
	}

	/**
	 * Contains just the connection shootdown & earlyClientLoginInSync trigger logic.
	 */
	private <X extends IHubClient> LinkedList<IWWRListener> earlyClientLoginWithReceiver(long uin, X client, ILoginReceiver<X> makeClient) {
		// -- Pass 1 --
		LinkedList<IWWRListener> wwrNotify = earlyClientLoginWithReceiverStep(client, makeClient);
		if (wwrNotify != null)
			return wwrNotify;

		// -- Shootdown --
		// If we get here, conflict happened. Are we allowed to perform connection shootdown?
		if (!config.allowConnectionShootdown.getValue())
			return null;
		// Ok, we are then. Take the shot.
		forceDisconnectUIN(uin, true);

		// -- Pass 2 --
		return earlyClientLoginWithReceiverStep(client, makeClient);
	}

	@Override
	public <X extends IHubClient> LoginResult loginUser(String username, String password, ILoginReceiver<X> makeClient) {
		// Q: Why is it okay to always close this?
		// A: hubLogin makes it's own reference.
		// Helps prevent endless "will it close" paranoia.
		try (INatsueUserData.LongTermPrivileged userData = usernameAndPasswordLookup(username, password, true)) {
			if (userData == null)
				return LoginResult.FAILED_AUTH;
			if (userData.isFrozen())
				return new LoginResult.AccountFrozen(getServerUIN(), userData.getBabelUserData());
			// Prepare client instance.
			X client = makeClient.receive(userData, this);
			if (client.getUserData() != userData)
				throw new RuntimeException("Client logging in tried to be cheeky and use the wrong user data root.");
			LinkedList<IWWRListener> wwrNotify = earlyClientLoginWithReceiver(userData.getUIN(), client, makeClient);
			if (wwrNotify == null)
				return new LoginResult.FailedConflict(userData.getBabelUserData());
			lateClientLogin(client, wwrNotify);
			return LoginResult.SUCCESS;
		}
	}

	@Override
	public void clientLogout(IHubClient cc) {
		LinkedList<IWWRListener> wwrNotify;
		synchronized (this) {
			wwrNotify = new LinkedList<IWWRListener>(users.wwrListeners);
			// Online flag must go false early to stop even more race conditions.
			cc.markNotReallyOnline();
		}
		// It's very important that this happens BEFORE we officially logout.
		// Otherwise, race condition, See the wwrNotify function's definition.
		wwrNotifyRun(false, cc, wwrNotify);
		synchronized (this) {
			users.earlyClientLogoutInSync(cc);
		}
	}

	@Override
	public void clientGiveMessage(IHubClient cc, long destinationUIN, PackedMessage message) {
		impGiveMessage(cc.getUserData(), destinationUIN, message);
	}

	@Override
	public void impGiveMessage(INatsueUserData userData, long destinationUIN, PackedMessage message) {
		// Just make sure of these here and now.
		long trueCauseUIN = userData.getUIN();
		message.senderUIN = trueCauseUIN;
		INatsueUserData destUserInfo = getUserDataByUIN(destinationUIN);
		if (destUserInfo == null) {
			rejectMessage(destinationUIN, message, "Non-existent user " + UINUtils.toString(destinationUIN));
			return;
		} else if (userData.isFrozen()) {
			rejectMessage(destinationUIN, message, "Source frozen");
			return;
		} else if (destUserInfo.isFrozen()) {
			rejectMessage(destinationUIN, message, "Destination frozen");
			return;
		}
		// Firewall modules.
		try {
			for (IFWModule fm : firewall)
				if (fm.handleMessage(userData, destUserInfo, message))
					return;
		} catch (Exception ex2) {
			// oh no you DON'T
			log(ex2);
			rejectMessage(destinationUIN, message, "Firewall threw exception");
			return;
		}
		// Send.
		sendMessage(destinationUIN, message, MsgSendType.Temp, trueCauseUIN);
	}

	@Override
	public void clientSendHistory(IHubClient cc, CreatureHistoryBlob history) {
		if (!config.allowCreatureHistory.getValue())
			return;
		String sanityError = history.verifySanity();
		if (sanityError == null) {
			int senderUID = UINUtils.uid(cc.getUIN());
			if (history.state != null) {
				// Initialize creature entry
				String cName = CreatureDataVerifier.stripName(config.messages, history.name);
				String cUserText = "";
				if (history.userText != null)
					cUserText = CreatureDataVerifier.stripUserText(config.messages, history.userText);
				database.ensureCreature(history.moniker, senderUID, history.state[0], history.state[1], history.state[2], history.state[3], history.state[4], cName, cUserText);
			} else {
				// Update name/user text fields
				String cName = null;
				String cUserText = null;
				if (!history.name.equals(""))
					cName = CreatureDataVerifier.stripName(config.messages, history.name);
				if (history.userText != null)
					cUserText = CreatureDataVerifier.stripUserText(config.messages, history.userText);
				if (cName != null || cUserText != null)
					database.updateCreatureText(senderUID, history.moniker, cName, cUserText);
			}
			for (LifeEvent le : history.events) {
				String a = CreatureDataVerifier.stripMonikerLike(le.mon1);
				String b = CreatureDataVerifier.stripMonikerLike(le.mon2);
				database.ensureCreatureEvent(senderUID, history.moniker, le.index, le.eventType, le.worldTime, le.ageTicks, le.unixTime, le.lifeStage, a, b, le.worldName, le.worldID, le.userID);
			}
		} else if (config.logHistorySanityFailures.getValue()) {
			log("History sanity failure from " + cc.getNickname() + ": " + sanityError);
		}
	}

	@Override
	public IHubUserDataCachePrivileged getUserDataCachePrivileged() {
		return userDataCache;
	}

	@Override
	public synchronized void considerRandomStatus(LongTerm user) {
		users.considerRandomStatusInSync(user);
	}

	@Override
	public synchronized String runSystemCheck(boolean detailed) {
		StringBuilder sb = new StringBuilder();
		// Preface
		if (detailed) {
			sb.append("-- Server Info --\n");
			sb.append("Version: " + SystemCommands.VERSION + "\n");
		} else {
			sb.append(SystemCommands.VERSION + "\n");
		}
		// Threads (if a detailed report)
		if (detailed) {
			sb.append("-- Threads --\n");
			for (Map.Entry<Thread, StackTraceElement[]> threads : Thread.getAllStackTraces().entrySet()) {
				sb.append(threads.getKey());
				sb.append("\n");
				for (StackTraceElement ste : threads.getValue()) {
					sb.append(" ");
					sb.append(ste);
					sb.append("\n");
				}
			}
		}
		// Random Pool
		if (detailed) {
			sb.append("-- Random Pool --\n");
			for (long entry : users.randomPool) {
				INatsueUserData nud = getUserDataByUIN(entry);
				if (nud == null) {
					sb.append(UINUtils.toString(entry) + " (unknown)\n");
				} else {
					sb.append(UINUtils.toString(entry) + " = " + nud.getNickname() + "\n");
				}
			}
		} else {
			sb.append("Random Pool:");
			for (long entry : users.randomPool)
				sb.append(" " + UINUtils.toString(entry));
			sb.append("\n");
		}
		// User Data Cache
		userDataCache.runSystemCheck(sb, detailed);
		// Connected Users
		if (detailed) {
			sb.append("-- Connected --\n");
		} else {
			sb.append("Connected:\n");
		}
		for (IHubClient entry : users.connectedClients.values())
			sb.append(UINUtils.toString(entry.getUIN()) + ": " + entry.getNickname() + "\n");
		// Quota Manager
		quotaManager.runSystemCheck(sb, detailed);
		// Cryo
		if (detailed)
			cryo.runSystemCheck(sb);
		return sb.toString();
	}

	@Override
	public synchronized ISessionClient acquireSessionClientForResearchCommands(long senderUIN) {
		// This is bad.
		IHubClient ihc = users.connectedClients.get(senderUIN);
		MainSessionState mss = (MainSessionState) ihc;
		return mss.client;
	}
}
