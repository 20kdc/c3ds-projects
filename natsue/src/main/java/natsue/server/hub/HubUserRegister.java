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

import natsue.server.hubapi.IHubClient;
import natsue.server.userdata.IHubUserDataCacheBetweenCacheAndHub;
import natsue.server.userdata.INatsueUserData;

/**
 * Exists because ServerHub got obscenely complicated.
 * Note that all inSync methods are relative to ServerHub synchronization.
 */
public class HubUserRegister {
	public final HashSet<IWWRListener> wwrListeners = new HashSet<>();
	public final HashMap<Long, IHubClient> connectedClients = new HashMap<>();
	public final ArrayList<Long> randomPool = new ArrayList<>();

	private final IHubUserDataCacheBetweenCacheAndHub userDataCache;
	public HubUserRegister(IHubUserDataCacheBetweenCacheAndHub udc) {
		userDataCache = udc;
	}

	/**
	 * Must run in synchronized block, or else events will come too early.
	 * Be wary that a successful return from here implies hubLogin has happened.
	 */
	public LinkedList<IWWRListener> earlyClientLoginInSync(IHubClient cc) {
		Long uin = cc.getUIN();
		if (connectedClients.containsKey(uin))
			return null;
		if (!userDataCache.hubLogin(cc.getUserData()))
			return null;
		// Past this point, hubLogin has occurred and we really, REALLY better not break this.
		connectedClients.put(uin, cc);
		if (!cc.isNoRandom())
			randomPool.add(uin);
		LinkedList<IWWRListener> wwrNotify = new LinkedList<IWWRListener>(wwrListeners);
		wwrListeners.add(cc);
		return wwrNotify;
	}

	/**
	 * The opposite to earlyClientLogin.
	 * Above all else, used when we need to "back out" of an otherwise confirmed client login.
	 */
	public void earlyClientLogoutInSync(IHubClient cc) {
		Long uin = cc.getUIN();
		randomPool.remove(uin);
		connectedClients.remove(uin, cc);
		wwrListeners.remove(cc);
		userDataCache.hubLogout(cc.getUserData());
	}

	/**
	 * Updates the random pool based on user flags.
	 */
	public void considerRandomStatusInSync(INatsueUserData.LongTerm user) {
		Long uin = user.getUIN(); 
		if (user.isNoRandom()) {
			System.out.println("no random");
			randomPool.remove(uin);
		} else {
			System.out.println("ya random");
			if (connectedClients.containsKey(uin))
				randomPool.add(uin);
		}
	}
}
