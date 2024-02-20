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
	 * Account is removed from random selection.
	 */
	public static final int FLAG_NO_RANDOM = 8;

	/**
	 * Account can receive Geats.
	 */
	public static final int FLAG_RECEIVE_GEATS = 16;

	/**
	 * Account is banned from global chat.
	 */
	public static final int FLAG_MUTED_GLOBAL_CHAT = 32;

	/**
	 * Account is unlisted, does not appear in "who".
	 */
	public static final int FLAG_UNLISTED = 64;

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
	 * Does this user opt-out of NET: RUSO?
	 */
	default boolean isNoRandom() {
		return (getFlags() & FLAG_NO_RANDOM) != 0;
	}

	/**
	 * Is this user willing to receive Geats (Genus 4)?
	 * NOTE: These can cause the Wasteland glitch.
	 */
	default boolean isReceivingGeats() {
		return (getFlags() & FLAG_RECEIVE_GEATS) != 0;
	}

	/**
	 * Is this user muted from global chat?
	 */
	default boolean isMutedGlobalChat() {
		return (getFlags() & FLAG_MUTED_GLOBAL_CHAT) != 0;
	}

	/**
	 * Is this user invisible in listings?
	 *
	 * This makes the user invisible to:
	 * 1. The JSON API
	 * 2. The website
	 * 3. The "who" command
	 *
	 * The user is NOT invisible to:
	 * 1. WWR notifications
	 * 2. Direct polling
	 */
	default boolean isUnlisted() {
		return (getFlags() & FLAG_UNLISTED) != 0;
	}

	/**
	 * This enum is used for dynamic flag monkey business 
	 */
	public enum Flag {
		admin(FLAG_ADMINISTRATOR, "For bonus RP points, call yourself a Warpshaper... ;)"),
		frozen(FLAG_FROZEN, "Prevents login and message receipt"),
		recvnb(FLAG_RECEIVE_NB_NORNS, "Allows receipt of NB norns"),
		norandom(FLAG_NO_RANDOM, "Disables random"),
		recvgeat(FLAG_RECEIVE_GEATS, "Allows receipt of geats"),
		muteglob(FLAG_MUTED_GLOBAL_CHAT, "Mutes the user in global chat"),
		unlisted(FLAG_UNLISTED, "The user is not visible in who/etc.");

		public final int value;
		public final String detail;

		Flag(int v, String d) {
			value = v;
			detail = d;
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
