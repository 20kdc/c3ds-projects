/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import natsue.data.babel.BabelClientVersion;
import natsue.server.userdata.INatsueUserData;

/**
 * Public interface exposed from IHubClient via ServerHub.getUserConnectionInfo
 */
public interface IHubClientAsSeenByOtherClients extends INatsueUserData.Proxy {
	/**
	 * Gets the client version.
	 */
	BabelClientVersion getClientVersion();

	/**
	 * Disjointly separate to isAdmin.
	 * Importantly, a user can be 2FA authed without being admin.
	 * This may come into play later.
	 */
	boolean has2FAAuthed();

	/**
	 * 
	 */
	boolean try2FAAuth(int code);
}
