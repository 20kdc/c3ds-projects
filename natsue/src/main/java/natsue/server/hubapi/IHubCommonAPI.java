/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import java.io.IOException;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.pm.PackedMessage;
import natsue.server.database.NatsueUserInfo;

/**
 * Represents the server.
 */
public interface IHubCommonAPI {
	/**
	 * Gets a UIN by nickname.
	 * The nickname will be automatically folded.
	 * Can and will return null.
	 */
	BabelShortUserData getShortUserDataByNickname(String name);

	/**
	 * Gets the name of a user by their UIN.
	 * Can and will return null.
	 */
	BabelShortUserData getShortUserDataByUIN(long uin);

	/**
	 * Returns true if the given UIN is online.
	 */
	boolean isUINOnline(long uin);

	/**
	 * Returns the flags for the given UIN. See NatsueUserInfo for these flags.
	 * Returns 0 on failure.
	 */
	int getUINFlags(long uin);

	default boolean isUINAdmin(long uin) {
		return (getUINFlags(uin) & NatsueUserInfo.FLAG_ADMINISTRATOR) != 0;
	}

	default boolean isUINReceivingNBNorns(long uin) {
		return (getUINFlags(uin) & NatsueUserInfo.FLAG_RECEIVE_NB_NORNS) != 0;
	}

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
