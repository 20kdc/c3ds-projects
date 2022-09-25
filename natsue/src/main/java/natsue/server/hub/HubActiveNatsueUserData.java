/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import natsue.data.babel.BabelShortUserData;
import natsue.server.hubapi.INatsueUserData;

/**
 * Used by ServerHub to keep data on active users up to date.
 */
class HubActiveNatsueUserData implements INatsueUserData.Root {
	public final BabelShortUserData babel;
	public volatile int flags;

	/**
	 * This data is carried along because flags & the password hash is updated at once.
	 */
	public volatile String pwHash;

	public HubActiveNatsueUserData(BabelShortUserData b, int f, String pws) {
		babel = b;
		flags = f;
		pwHash = pws;
	}

	@Override
	public BabelShortUserData getBabelUserData() {
		return babel;
	}

	@Override
	public int getFlags() {
		return flags;
	}
}
