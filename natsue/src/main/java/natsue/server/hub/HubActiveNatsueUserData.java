/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.util.concurrent.atomic.AtomicInteger;

import natsue.data.babel.BabelShortUserData;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.names.PWHash;
import natsue.server.database.NatsueDBUserInfo;
import natsue.server.userdata.INatsueUserData;

/**
 * Used by ServerHub to keep data on active users up to date.
 */
class HubActiveNatsueUserData implements INatsueUserData.LongTermPrivileged, ILogSource {
	/**
	 * Babel user data.
	 */
	public final BabelShortUserData babel;
	/**
	 * Folded nickname.
	 */
	public final String nicknameFolded;

	private final int uid;
	private volatile int flags;
	private final AtomicInteger refCount = new AtomicInteger();
	private final HubUserDataCache parent;
	private final boolean logged;

	private volatile String pwHash;
	private volatile boolean isDead = false;
	// While synchronized, this exists to help with lock tracking
	private volatile String activity;

	public HubActiveNatsueUserData(HubUserDataCache p, NatsueDBUserInfo ui) {
		babel = ui.convertToBabel();
		nicknameFolded = ui.nicknameFolded;
		uid = ui.uid;
		flags = ui.flags;
		pwHash = ui.passwordHash;
		parent = p;
		logged = parent.config.logUserCacheManagement.getValue();
	}

	public String debugGetStatus() {
		String sv = activity;
		if (sv == null)
			sv = "not busy";
		return refCount.get() + " refs, " + sv;
	}

	@Override
	public ILogProvider getLogParent() {
		return parent.getLogParent();
	}

	@Override
	public String toString() {
		return "UserData[" + nicknameFolded + "]";
	}

	@Override
	public BabelShortUserData getBabelUserData() {
		return babel;
	}

	@Override
	public String getNicknameFolded() {
		return nicknameFolded;
	}

	@Override
	public int getFlags() {
		return flags;
	}

	@Override
	public LongTermPrivileged open(String site) {
		int res = refCount.incrementAndGet();
		if (logged)
			log("++ (" + res + ") @ " + site);
		return this;
	}

	@Override
	public void close() {
		int res = refCount.decrementAndGet();
		if (logged)
			log("-- (" + res + ")");
		if (res < 0) {
			log("< 0 reference count in HubActiveNatsueUserData");
		} else if (res == 0) {
			synchronized (this) {
				isDead = true;
				parent.notifyZeroRefCount(this);
			}
		}
	}

	@Override
	public String getPasswordHash() {
		return pwHash;
	}

	@Override
	public boolean setPassword(String password) {
		synchronized (this) {
			if (isDead)
				return false;
			activity = "updating password";
			String newHash = PWHash.hash(uid, password);
			if (parent.database.updateUserAuth(uid, newHash, flags)) {
				pwHash = newHash;
				activity = null;
				return true;
			}
			activity = null;
			return false;
		}
	}

	@Override
	public boolean updateFlags(int and, int xor) {
		synchronized (this) {
			if (isDead)
				return false;
			activity = "updating flags";
			int newFlags = (flags & and) ^ xor;
			if (parent.database.updateUserAuth(uid, pwHash, newFlags)) {
				flags = newFlags;
				activity = null;
				return true;
			}
			activity = null;
			return false;
		}
	}
}
