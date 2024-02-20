/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

/**
 * Public interface exposed from IHubClient via ServerHub.getUserConnectionInfo
 */
public interface IHubClientAsSeenByOtherClientsPrivileged extends IHubClientAsSeenByOtherClients {
	/**
	 * Attempt to 2FA-auth this connection.
	 */
	boolean try2FAAuth(int code);
}
