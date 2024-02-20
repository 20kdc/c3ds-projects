/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.userdata;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;
import natsue.names.NicknameVerifier;
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
	default String getNickname() {
		return getBabelUserData().nickname;
	}

	/**
	 * Gets the folded nickname of this user.
	 * Immutable.
	 */
	String getNicknameFolded();

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
	 * Used for long-term instances.
	 * These are reference-counted so that the user data cache can efficiently flush them.
	 * open() adds a reference, while close() drops it.
	 */
	public interface LongTerm extends Root, AutoCloseable {
		/**
		 * Returns itself. The opposite to close().
		 * site is expected to be a constant string.
		 */
		LongTerm open(String site);

		// override to remove exceptions
		@Override
		void close();
	}

	/**
	 * Used for long-term instances.
	 * These are reference-counted so that the user data cache can efficiently flush them.
	 * open() adds a reference, while close() drops it.
	 */
	public interface LongTermPrivileged extends LongTerm {
		@Override
		LongTermPrivileged open(String site);

		/**
		 * Gets the password hash. Possibly null (blocks logging in)
		 */
		String getPasswordHash();

		/**
		 * Calculates the 2FA secret, assuming the password is known.
		 * Returns null if there is no 2FA secret.
		 */
		byte[] calculate2FASecret(String password);

		/**
		 * Updates the account's password.
		 * This also disables 2FA.
		 */
		boolean setPassword(String password);

		/**
		 * Updates the account's flags.
		 */
		boolean updateFlags(int and, int xor);

		/**
		 * Sets account flags.
		 */
		default boolean setFlags(int flags) {
			return updateFlags(~flags, flags);
		}

		/**
		 * Unsets account flags.
		 */
		default boolean unsetFlags(int flags) {
			return updateFlags(~flags, 0);
		}

		/**
		 * Update 2FA seed to this value.
		 */
		boolean update2FA(long value);
	}

	/**
	 * Used to make sure nobody does anything stupid with a proxy.
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
		default String getNicknameFolded() {
			return getUserData().getNicknameFolded();
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
	 * Long-term methods do nothing.
	 */
	public static class Fixed implements LongTermPrivileged {
		/**
		 * Babel user data.
		 */
		public final BabelShortUserData babel;
		/**
		 * Folded nickname.
		 */
		public final String nicknameFolded;
		/**
		 * Account flags.
		 */
		public final int flags;

		/**
		 * Intended for use by fixed identities, so auto-folds nicknames!
		 */
		public Fixed(BabelShortUserData b, int f) {
			babel = b;
			nicknameFolded = NicknameVerifier.foldNickname(b.nickname);
			flags = f;
		}

		/**
		 * General DB to general converter.
		 */
		public Fixed(NatsueDBUserInfo ui) {
			babel = ui.convertToBabel();
			nicknameFolded = ui.nicknameFolded;
			flags = ui.flags;
		}

		@Override
		public BabelShortUserData getBabelUserData() {
			return babel;
		}

		@Override
		public String getNicknameFolded() {
			return nicknameFolded;
		}

		@Override
		public int getFlags() {
			return flags;
		}

		@Override
		public LongTermPrivileged open(String site) {
			return this;
		}

		@Override
		public void close() {
		}

		@Override
		public String getPasswordHash() {
			return null;
		}

		@Override
		public boolean setPassword(String password) {
			return false;
		}

		@Override
		public byte[] calculate2FASecret(String password) {
			return null;
		}

		@Override
		public boolean update2FA(long value) {
			return false;
		}

		@Override
		public boolean updateFlags(int and, int xor) {
			return false;
		}
	}
}
