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
		return (getFlags() & FLAG_ADMINISTRATOR) != 0;
	}

	/**
	 * Is this user frozen (banned)?
	 */
	default boolean isFrozen() {
		return (getFlags() & FLAG_FROZEN) != 0;
	}

	/**
	 * Is this user willing to receive Norns with species values other than 1 and 2?
	 * NOTE: Said Norns can crash people without necessary client mods
	 */
	default boolean isReceivingNBNorns() {
		return (getFlags() & FLAG_RECEIVE_NB_NORNS) != 0;
	}

	/**
	 * This enum is used for dynamic flag monkey business 
	 */
	public enum Flag {
		admin(FLAG_ADMINISTRATOR),
		frozen(FLAG_FROZEN),
		recvnb(FLAG_RECEIVE_NB_NORNS);
		public final int value;

		Flag(int v) {
			value = v;
		}

		/**
		 * Returns the flag enum value for the given power of two.
		 * Returns null if it doesn't exist.
		 */
		public static Flag getFlagByPower(int v) {
			for (Flag fv : values()) {
				if (fv.value == v)
					return fv;
			}
			return null;
		}

		/**
		 * Writes out flags into a convenient list.
		 */
		public static String showFlags(int ofValue) {
			StringBuilder sb = new StringBuilder();
			int p2 = 1;
			boolean first = true;
			for (int iteration = 0; iteration < 32; iteration++) {
				if ((ofValue & p2) != 0) {
					if (!first)
						sb.append(' ');
					first = false;
					Flag f = getFlagByPower(p2);
					if (f == null) {
						sb.append(Integer.toString(p2));
					} else {
						sb.append(f.name().toUpperCase());
					}
				}
				p2 <<= 1;
			}
			return sb.toString();
		}
	}
}
