/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.hubapi;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;
import natsue.server.database.INatsueUserFlags;
import natsue.server.database.NatsueDBUserInfo;

/**
 * Represents the data of a Natsue user.
 * Note the relations:
 * NatsueDBUserInfo: Immutable data structure for sending to/from database, uses UIDs not UINs.
 * BabelShortUserData: Immutable protocol data structure that client expects.
 */
public interface INatsueUserData extends INatsueUserFlags {
	/**
	 * Gets the BabelShortUserData of this user.
	 * Immutable.
	 */
	BabelShortUserData getBabelUserData();

	/**
	 * Gets the nickname of this user.
	 * Immutable.
	 */
	default String getNickName() {
		return getBabelUserData().nickName;
	}

	/**
	 * Gets the UIN of this user.
	 * Immutable.
	 */
	default long getUIN() {
		return getBabelUserData().uin;
	}

	/**
	 * Gets the UIN of this user, as a string.
	 * Immutable.
	 */
	default String getUINString() {
		return UINUtils.toString(getBabelUserData().uin);
	}

	/**
	 * Used to make sure nobody does anything stupid with a proxy.
	 * ServerHub needs to get ahold of the root so it can make sure to update it's cached flags.
	 */
	public interface Root extends INatsueUserData {
		
	}

	public interface Proxy extends INatsueUserData {
		/**
		 * Returns the underlying INatsueUserData object.
		 */
		Root getUserData();

		@Override
		default BabelShortUserData getBabelUserData() {
			return getUserData().getBabelUserData();
		}

		@Override
		default int getFlags() {
			return getUserData().getFlags();
		}
	}

	/**
	 * Fixed-content user data.
	 * Used for system users always.
	 * Used for regular users as a "snapshot" when they're offline.
	 */
	public static class Fixed implements Root {
		public final BabelShortUserData babel;
		public final int flags;
		public Fixed(BabelShortUserData b, int f) {
			babel = b;
			flags = f;
		}

		public Fixed(NatsueDBUserInfo ui) {
			babel = ui.convertToBabel();
			flags = ui.flags;
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
}
