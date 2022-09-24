/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import java.util.function.Function;

import natsue.data.babel.BabelShortUserData;
import natsue.server.database.NatsueUserInfo;

/**
 * This is just the login APIs - in practice this is part of IHubLoginClientAPI or IHubPrivilegedAPI.
 * This also serves as a refactoring point so that who you're logging in as can decide a different hub in some future version.
 */
public interface IHubLoginAPI {
	/**
	 * Logs in a user.
	 * confirm is expected to return an IHubClient.
	 * Returns true if the whole process succeeded, false otherwise.
	 */
	<X extends IHubClient> LoginResult loginUser(String username, String password, ILoginReceiver<X> makeClient);

	public interface ILoginReceiver<X extends IHubClient> {
		/**
		 * Called to construct the IHubClient.
		 * This is not confirmation of success.
		 */
		X receive(BabelShortUserData userData, IHubClientAPI clientAPI);
		/**
		 * Called to confirm success, just before the hub can start calls on the client.
		 */
		void confirm(X result);
	}

	public static class LoginResult {
		public static final LoginResult SUCCESS = new LoginResult();
		public static final LoginResult FAILED_AUTH = new LoginResult();
		public static class FailedConflict extends LoginResult {
			public final NatsueUserInfo who;
			public FailedConflict(NatsueUserInfo ui) {
				who = ui;
			}
		}
		public static class AccountFrozen extends LoginResult {
			public final long serverUIN;
			public final NatsueUserInfo who;
			public AccountFrozen(long suin, NatsueUserInfo ui) {
				serverUIN = suin;
				who = ui;
			}
		}
	}
}
