/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import natsue.server.userdata.IHubUserDataCache;

/**
 * Represents the server.
 */
public interface IHubCommonAPI extends IHubUserDataCache {

	/**
	 * Returns true if the given UIN is online.
	 */
	boolean isUINOnline(long uin);

	/**
	 * Returns true if the given UIN is an admin.
	 */
	boolean isUINAdmin(long targetUIN);

	/**
	 * Gets a UIN reserved for this server.
	 */
	long getServerUIN();

	/**
	 * Returns a random online UIN that isn't the system and isn't whoIsnt.
	 * Returns 0 if none could be found.
	 */
	long getRandomOnlineNonSystemUIN(long whoIsnt);
}
