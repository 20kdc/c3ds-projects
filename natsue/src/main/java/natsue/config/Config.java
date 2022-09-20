/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

/**
 * All the config for everything everywhere.
 */
public class Config extends BaseConfig {
	/**
	 * Used to store the DB and config separately.
	 */
	public final Str actualDB = new Str("actualDB", "");

	/**
	 * Used to store the DB and config separately.
	 */
	public final Int port = new Int("port", 49152);

	/**
	 * Maximum length of a creature name.
	 * Default chosen by the amount of the letter 'i' you can put in a name, with some rounding up.
	 */
	public final Int maxCreatureNameLen = new Int("maxCreatureNameLen", 32);

	/**
	 * Maximum length of a creature's user text.
	 * Default chosen by the amount of the letter 'i' you can put in a name, with some rounding up.
	 */
	public final Int maxCreatureUserTextLen = new Int("maxCreatureUserTextLen", 896);

	/**
	 * Maximum size of the username/password section of a handshake.
	 */
	public final Int maxLoginInfoSize = new Int("maxLoginInfoSize", 0x1000);

	/**
	 * Maximum message size. Need to be careful with this as it's an upper bound on creature sizes.
	 * Can always have other limits.
	 */
	public final Int maxBabelBinaryMessageSize = new Int("maxBabelBinaryMessageSize", 0x1000000);

	/**
	 * Maximum size for the further data of "unknown-ish" packets.
	 */
	public final Int maxUnknownCTOSFurtherDataSize = new Int("maxUnknownCTOSFurtherDataSize", 0x10000);

	/**
	 * Maximum size for creature history packets.
	 */
	public final Int maxFeedHistorySize = new Int("maxFeedHistorySize", 0x10000);

	/**
	 * Maximum creature history event count.
	 */
	public final Int maxCreatureHistoryEvents = new Int("maxCreatureHistoryEvents", 1024);

	/**
	 * Maximum size of an untrusted decompressed PRAY file
	 */
	public final Int maxDecompressedPRAYSize = new Int("maxDecompressedPRAYSize", 0x1000000);

	/**
	 * Allow registration.
	 */
	public final Bool allowRegistration = new Bool("allowRegistration", true);

	/**
	 * Allow creature history.
	 */
	public final Bool allowCreatureHistory = new Bool("allowCreatureHistory", true);

	/**
	 * Log failed authentication attempts.
	 */
	public final Bool logFailedAuthentication = new Bool("logFailedAuthentication", true);

	/**
	 * Log all connections.
	 */
	public final Bool logAllConnections = new Bool("logAllConnections", true);

	/**
	 * Log all CTOS packets.
	 */
	public final Bool logAllIncomingPackets = new Bool("logAllIncomingPackets", true);

	/**
	 * Log history parsing or sanity failures
	 */
	public final Bool logHistorySanityFailures = new Bool("logHistorySanityFailures", true);

	/**
	 * Log expected database errors
	 */
	public final Bool logExpectedDBErrors = new Bool("logExpectedDBErrors", true);

	/**
	 * Log pings.
	 */
	public final Bool logPings = new Bool("logPings", true);
}
