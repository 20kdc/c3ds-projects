/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import natsue.config.IConfigProvider;
import natsue.data.babel.UINUtils;

/**
 * Abstract interface to enforce clean separation of SQL-handling code from everything else.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public interface INatsueDatabase extends IConfigProvider {
	/**
	 * Gets a user by UID.
	 * Returns null on failure.
	 */
	UserInfo getUserByUID(int uid);

	/**
	 * Gets a user by username.
	 * Returns null on failure.
	 */
	UserInfo getUserByUsername(String username);

	public static class UserInfo {
		public final String username;
		/**
		 * Hex-encoded lowercase sha256 hash of the password.
		 */
		public final String passwordHash;
		public final int uid;

		public UserInfo(String u, String p, int ui) {
			username = u;
			passwordHash = p;
			uid = ui;
		}

		public long getUIN() {
			return UINUtils.make(uid, UINUtils.HID_USER);
		}
	}
}
