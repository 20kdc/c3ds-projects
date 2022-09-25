/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

/**
 * Mixin for classes that contain Natsue user flags.
 */
public interface INatsueUserFlags {
	/**
	 * Administrator.
	 */
	public static final int FLAG_ADMINISTRATOR = 1;

	/**
	 * Account has been frozen.
	 */
	public static final int FLAG_FROZEN = 2;

	/**
	 * Account can receive NB norns.
	 */
	public static final int FLAG_RECEIVE_NB_NORNS = 4;

	/**
	 * Gets the flags of this user.
	 * These can be mutated by modUserFlags on regular users.
	 * See INatsueUserFlags for flag values.
	 */
	int getFlags();

	/**
	 * Is this user an administrator?
	 */
	default boolean isAdmin() {
		return (getFlags() & NatsueDBUserInfo.FLAG_ADMINISTRATOR) != 0;
	}

	/**
	 * Is this user frozen (banned)?
	 */
	default boolean isFrozen() {
		return (getFlags() & NatsueDBUserInfo.FLAG_FROZEN) != 0;
	}

	/**
	 * Is this user willing to receive Norns with species values other than 1 and 2?
	 * NOTE: Said Norns can crash people without necessary client mods
	 */
	default boolean isReceivingNBNorns() {
		return (getFlags() & NatsueDBUserInfo.FLAG_RECEIVE_NB_NORNS) != 0;
	}
}
