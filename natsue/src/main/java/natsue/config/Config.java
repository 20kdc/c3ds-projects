/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

import natsue.server.firewall.FirewallLevel;

/**
 * All the config for everything everywhere.
 */
public class Config extends BaseConfig.Group {
	/**
	 * Database configuration
	 */
	public final ConfigDB db = new ConfigDB();

	/**
	 * Used to store the DB and config separately.
	 */
	public final Int port = new Int("port", 49152)
			.describe("TCP port on which the server should listen.");

	/**
	 * Nickname details
	 */
	public final ConfigAccounts accounts = new ConfigAccounts();

	/**
	 * Maximum amount of spooled messages to send back to a connecting client.
	 */
	public final Int maxSpoolToReadOnConnect = new Int("maxSpoolToReadOnConnect", 0x1000)
			.describe("Maximum amount of spooled messages to send back to a connecting client.\nIf the amount exceeds this, they have to reconnect to get the rest.");

	/**
	 * Maximum length of a creature name.
	 * Default chosen by the amount of the letter 'i' you can put in a name, with some rounding up.
	 */
	public final Int maxCreatureNameLen = new Int("maxCreatureNameLen", 32)
			.describe("Maximum length of a creature name.");

	/**
	 * Maximum length of a creature's user text.
	 * Default chosen by the amount of the letter 'i' you can put in a name, with some rounding up.
	 */
	public final Int maxCreatureUserTextLen = new Int("maxCreatureUserTextLen", 896)
			.describe("Maximum length of a creature's user text.");

	/**
	 * Maximum size of the username/password section of a handshake.
	 */
	public final Int maxLoginInfoSize = new Int("maxLoginInfoSize", 0x1000)
			.describe("Maximum size of the username/password section of a login packet.");

	/**
	 * Maximum message size. Need to be careful with this as it's an upper bound on creature sizes.
	 * Can always have other limits.
	 */
	public final Int maxBabelBinaryMessageSize = new Int("maxBabelBinaryMessageSize", 0x1000000)
			.describe("Maximum Babel binary message size. Be careful: This is also a maximum size for creatures!");

	/**
	 * Maximum size for the further data of "unknown-ish" packets.
	 * Practically, used when discarding Virtual Circuit packets.
	 */
	public final Int maxUnknownCTOSFurtherDataSize = new Int("maxUnknownCTOSFurtherDataSize", 0x10000)
			.describe("Maximum unknown CTOS further data size. Practically, used when discarding Virtual Circuit packets.");

	/**
	 * Maximum size for creature history packets.
	 */
	public final Int maxFeedHistorySize = new Int("maxFeedHistorySize", 0x10000)
			.describe("Maximum size of a creature history blob.");

	/**
	 * Maximum creature history event count.
	 */
	public final Int maxCreatureHistoryEvents = new Int("maxCreatureHistoryEvents", 1024)
			.describe("Maximum amount of creature history events that may be uploaded at once.");

	/**
	 * Maximum size of an untrusted decompressed PRAY file
	 */
	public final Int maxDecompressedPRAYSize = new Int("maxDecompressedPRAYSize", 0x1000000)
			.describe("Maximum size of the total decompressed blocks in a PRAY file. Be careful: This is also a maximum size for creatures!");

	/**
	 * Exclude yourself from NET: RUSO
	 */
	public final Bool excludeSelfRUSO = new Bool("excludeSelfRUSO", true)
			.describe("Excludes yourself from NET: RUSO random user requests, despite this being a documented behaviour of the command.");

	/**
	 * Text to send someone whose account was frozen
	 */
	public final Str accountFrozenText = new Str("accountFrozenText", "Your account has been frozen.\nPlease contact administration out of game for further details.")
			.describe("Text to send to people whose accounts have been frozen.");

	/**
	 * Allow a connecting user to disconnect their existing connection forcibly
	 */
	public final Bool allowConnectionShootdown = new Bool("allowConnectionShootdown", true)
			.describe("Allows a connecting user to disconnect their existing connection forcibly (i.e. timeouts, etc.)");

	/**
	 * Manual keepalive because SO_KEEPALIVE just isn't up to snuff
	 */
	public final Int manualKeepAliveTime = new Int("manualKeepAliveTime", 30)
			.describe("Amount of seconds to go without receiving a packet from the client before we decide to just send a little one to it. <= 0 means not to send these.");

	/**
	 * Request abuse prevention
	 */
	public final Int initialNoDataShutdownTime = new Int("initialNoDataShutdownTime", 30)
			.describe("For an initial connection, amount of seconds to go without receiving a byte from the client before we decide we're being trolled.");

	/**
	 * Request abuse prevention
	 */
	public final Int httpRequestNoDataShutdownTime = new Int("httpRequestNoDataShutdownTime", 30)
			.describe("During an HTTP request, amount of seconds to go without receiving a byte from the client before we decide we're being trolled.");

	/**
	 * Request abuse prevention
	 */
	public final Int httpRequestFakeLingerTime = new Int("httpRequestFakeLingerTime", 10)
			.describe("During an HTTP request, the linger time for transmitting data back to the client.");

	/**
	 * Allow creature history.
	 */
	public final Bool allowCreatureHistory = new Bool("allowCreatureHistory", true)
			.describe("Stores creature history in the database.");

	/**
	 * Firewall level
	 */
	public final Emu<FirewallLevel> firewallLevel = new Emu<>("firewallLevel", FirewallLevel.vanillaSafe)
			.describe("Enables the complex firewall, used to block potentially dangerous PRAY files.\n" +
					"Options are:\n" +
					"minimal (almost nothing),\n" +
					"vanillaSafe (blocks known dangerous PRAY blocks, + NB norn check),\n" +
					"full (block any blocks vanilla wouldn't send, + NB norn check),\n" +
					"rejectAll (TESTING ONLY, REJECTS EVERYTHING)");

	/**
	 * Log failed authentication attempts.
	 */
	public final Bool logFailedAuthentication = new Bool("logFailedAuthentication", true)
			.describe("Log failed authentication attempts.");

	/**
	 * Log all connections.
	 */
	public final Bool logAllConnections = new Bool("logAllConnections", true)
			.describe("Log all connections.");

	/**
	 * Log all CTOS packets.
	 */
	public final Bool logAllIncomingPackets = new Bool("logAllIncomingPackets", true)
			.describe("Log all incoming packets.");

	/**
	 * Log history parsing or sanity failures
	 */
	public final Bool logHistorySanityFailures = new Bool("logHistorySanityFailures", true)
			.describe("Log history sanity failures.");

	/**
	 * Log pings.
	 */
	public final Bool logPings = new Bool("logPings", true)
			.describe("Log pings.");
}
