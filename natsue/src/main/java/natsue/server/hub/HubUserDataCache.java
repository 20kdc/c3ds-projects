/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import natsue.config.ConfigAccounts;
import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.names.NicknameVerifier;
import natsue.names.PWHash;
import natsue.server.database.UnixTime;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.NatsueDBUserInfo;
import natsue.server.userdata.IHubUserDataCacheBetweenCacheAndHub;
import natsue.server.userdata.INatsueUserData;
import natsue.server.userdata.INatsueUserData.Fixed;
import natsue.server.userdata.INatsueUserData.Root;

/**
 * Responsible for caching HubActiveNatsueUserData.
 * More importantly, responsible for keeping this cache up to date.
 */
public class HubUserDataCache implements IHubUserDataCacheBetweenCacheAndHub, ILogSource {
	public final INatsueDatabase database;
	public final ConfigAccounts config;
	private final HashMap<Long, INatsueUserData.LongTermPrivileged> cacheByUIN = new HashMap<>();
	private final HashMap<String, INatsueUserData.LongTermPrivileged> cacheByNick = new HashMap<>();
	private final ILogProvider logParent;

	/**
	 * Apparently thread-safe, who knew?
	 */
	private final Random randomGen = new Random();

	public HubUserDataCache(INatsueDatabase db, ConfigAccounts cfg, ILogProvider lp) {
		database = db;
		config = cfg;
		logParent = lp;
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	// -- Short-Term Getters --
	// These are specifically specialized rather than derived from the long-term getters.
	// This is to help prevent locking on database requests - any snapshot will do after all.

	@Override
	public INatsueUserData getUserDataByNickname(String name) {
		name = NicknameVerifier.foldNickname(name);
		synchronized (this) {
			INatsueUserData.LongTermPrivileged ihc = cacheByNick.get(name);
			if (ihc != null)
				return ihc;
		}
		// Ok, now check with database
		if (!NicknameVerifier.verifyNickname(config, name))
			return null;
		NatsueDBUserInfo db = database.getUserByFoldedNickname(name);
		if (db != null)
			return new INatsueUserData.Fixed(db);
		return null;
	}

	@Override
	public INatsueUserData getUserDataByUIN(long uin) {
		synchronized (this) {
			INatsueUserData.LongTermPrivileged ihc = cacheByUIN.get(uin);
			if (ihc != null)
				return ihc;
		}
		NatsueDBUserInfo db = database.getUserByUIN(uin);
		if (db != null)
			return new INatsueUserData.Fixed(db);
		return null;
	}

	// --------------------

	/**
	 * From within one of the next two functions, prepare a NatsueDBUserInfo, if any.
	 */
	private INatsueUserData.LongTermPrivileged inSyncIntroduceIntoDatabase(NatsueDBUserInfo userInfo) {
		if (userInfo == null)
			return null;
		HubActiveNatsueUserData ud = new HubActiveNatsueUserData(this, userInfo);
		ud.open("inSyncIntroduceIntoDatabase");
		// NOTE: Information from the DB is always right.
		cacheByUIN.put(ud.babel.uin, ud);
		cacheByNick.put(ud.nicknameFolded, ud);
		return ud;
	}

	@Override
	public synchronized INatsueUserData.LongTermPrivileged openUserDataByNicknameLT(String name) {
		name = NicknameVerifier.foldNickname(name);
		INatsueUserData.LongTermPrivileged ihc = cacheByNick.get(name);
		if (ihc != null)
			return ihc.open("openUserDataByNicknameLT");
		// Ok, now check with database
		if (!NicknameVerifier.verifyNickname(config, name))
			return null;
		return inSyncIntroduceIntoDatabase(database.getUserByFoldedNickname(name));
	}

	private synchronized INatsueUserData.LongTermPrivileged openUserDataByNicknameFVLT(String name) {
		INatsueUserData.LongTermPrivileged ihc = cacheByNick.get(name);
		if (ihc != null)
			return ihc.open("openUserDataByNicknameFVLT");
		return inSyncIntroduceIntoDatabase(database.getUserByFoldedNickname(name));
	}

	@Override
	public synchronized INatsueUserData.LongTermPrivileged openUserDataByUINLT(long uin) {
		INatsueUserData.LongTermPrivileged ihc = cacheByUIN.get(uin);
		if (ihc != null)
			return ihc.open("openUserDataByUINLT");
		return inSyncIntroduceIntoDatabase(database.getUserByUIN(uin));
	}

	private INatsueUserData.LongTermPrivileged openUserdataRegisterFV(String username, String usernameFolded, String password, boolean allowedToRegister) {
		INatsueUserData.LongTermPrivileged ui = openUserDataByNicknameFVLT(usernameFolded);
		if ((ui != null) || !allowedToRegister)
			return ui;
		// If we fail this too many times, the DB's dead
		for (int i = 0; i < config.registrationAttempts.getValue(); i++) {
			// Apparently thread-safe, who knew?
			int uid = randomGen.nextInt();
			// don't register with 0 or negative numbers
			// negative numbers will probably fry the Warp inbox system!!!
			if (uid <= 0)
				continue;
			NatsueDBUserInfo newUI = new NatsueDBUserInfo(uid, username, usernameFolded, PWHash.hash(uid, password), 0, UnixTime.get());
			boolean success = database.tryCreateUser(newUI);
			if (success)
				log("Registered user: " + username + " as UID " + uid);
			// It's possible that a username collision occurred during the registration process.
			// In that event, we obviously should be seeing a username here.
			// Alternatively, we need to read in the result of our success.
			ui = openUserDataByNicknameFVLT(usernameFolded);
			if (ui != null)
				break;
		}
		return ui;
	}

	@Override
	public INatsueUserData.LongTermPrivileged usernameAndPasswordLookup(String username, String password, boolean allowedToRegister) {
		String usernameFolded = NicknameVerifier.foldNickname(username);
		if (!NicknameVerifier.verifyNickname(config, usernameFolded))
			return null;
		try (INatsueUserData.LongTermPrivileged ui = openUserdataRegisterFV(username, usernameFolded, password, allowedToRegister && config.allowRegistration.getValue())) {
			if (ui == null)
				return null;
			// try-with-resources will always close the existing reference
			// (makes the code a lot cleaner though)
			// so make sure to open a new one if we succeed
			String pwHash = ui.getPasswordHash();
			if (UINUtils.isRegularUser(ui.getUIN()) && pwHash != null)
				if (PWHash.verify(UINUtils.uid(ui.getUIN()), pwHash, password))
					return ui.open("usernameAndPasswordLookup");
		}
		return null;
	}

	public synchronized void notifyZeroRefCount(HubActiveNatsueUserData hubActiveNatsueUserData) {
		cacheByUIN.remove(hubActiveNatsueUserData.babel.uin, hubActiveNatsueUserData);
		cacheByNick.remove(hubActiveNatsueUserData.nicknameFolded, hubActiveNatsueUserData);
	}

	@Override
	public synchronized boolean hubLogin(Root root) {
		if (root instanceof Fixed) {
			Fixed fixed = (Fixed) root;
			if (cacheByUIN.putIfAbsent(fixed.babel.uin, fixed) == null) {
				cacheByNick.put(fixed.nicknameFolded, fixed);
				return true;
			}
			return false;
		} else if (root instanceof HubActiveNatsueUserData) {
			((HubActiveNatsueUserData) root).open("hubLogin");
			return true;
		} else {
			throw new RuntimeException("Unhandled root type");
		}
	}

	@Override
	public synchronized void hubLogout(Root root) {
		if (root instanceof Fixed) {
			Fixed fixed = (Fixed) root;
			cacheByUIN.remove(fixed.babel.uin, fixed);
			cacheByNick.remove(fixed.nicknameFolded, fixed);
		} else if (root instanceof HubActiveNatsueUserData) {
			((HubActiveNatsueUserData) root).close();
		} else {
			throw new RuntimeException("Unhandled root type");
		}
	}

	@Override
	public synchronized void runSystemCheck(StringBuilder sb) {
		sb.append("UDC Content:\n");
		for (Map.Entry<Long, INatsueUserData.LongTermPrivileged> ent : cacheByUIN.entrySet()) {
			INatsueUserData.LongTermPrivileged v = ent.getValue();
			sb.append(UINUtils.toString(ent.getKey()) + ": " + v.getNickname() + ", ");
			if (v instanceof Fixed) {
				sb.append("FIXED\n");
			} else if (v instanceof HubActiveNatsueUserData) {
				sb.append(((HubActiveNatsueUserData) v).debugGetRefCount() + " refs\n");
			}
		}
	}
}
