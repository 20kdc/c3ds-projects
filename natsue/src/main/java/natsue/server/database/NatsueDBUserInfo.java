/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;
import natsue.server.userdata.INatsueUserData;

/**
 * Information on a user. 
 */
public class NatsueDBUserInfo implements INatsueUserFlags {
	public final String nickname, nicknameFolded;
	/**
	 * Hex-encoded lowercase sha256 hash of the password.
	 */
	public final String passwordHash;
	public final int uid;
	public final int flags;

	public NatsueDBUserInfo(int ui, String n, String nf, String p, int f) {
		uid = ui;
		nickname = n;
		nicknameFolded = nf;
		passwordHash = p;
		flags = f;
	}

	public long getUIN() {
		return UINUtils.ofRegularUser(uid);
	}

	public int getFlags() {
		return flags;
	}

	public BabelShortUserData convertToBabel() {
		return new BabelShortUserData("", "", nickname, UINUtils.ofRegularUser(uid));
	}
}
