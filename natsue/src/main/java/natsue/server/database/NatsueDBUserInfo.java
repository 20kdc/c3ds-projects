/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;

/**
 * Information on a user. 
 */
public class NatsueDBUserInfo implements INatsueUserFlags {
	public final String nickname, nicknameFolded;
	/**
	 * See PWHash.
	 */
	public final String passwordHash;
	public final int uid;
	public final int flags;
	public final long creationUnixTime;
	/**
	 * 2FA seed. 0 indicates invalid.
	 * If 2FA is required for dangerous commands is managed on a per-account level.
	 * This is because bootstrapping 2FA across the admin accounts can be kind of hazardous.
	 */
	public final long twoFactorSeed;

	public NatsueDBUserInfo(int ui, String n, String nf, String p, int f, long l, long t) {
		uid = ui;
		nickname = n;
		nicknameFolded = nf;
		passwordHash = p;
		flags = f;
		creationUnixTime = l;
		twoFactorSeed = t;
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
