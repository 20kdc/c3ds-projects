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
	 * Message sizes and such
	 */
	public final ConfigMessages messages = new ConfigMessages();

	/**
	 * Nickname details
	 */
	public final ConfigAccounts accounts = new ConfigAccounts();

	/**
	 * Quota details
	 */
	public final ConfigConnectionQuotas connectionQuotas = new ConfigConnectionQuotas();

	/**
	 * Maximum amount of spooled messages to send back to a connecting client.
	 */
	public final Int maxSpoolToReadOnConnect = new Int("maxSpoolToReadOnConnect", 0x1000)
			.describe("Maximum amount of spooled messages to send back to a connecting client.\nIf the amount exceeds this, they have to reconnect to get the rest.");

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
	 * Allow remote HTTP API use
	 */
	public final Bool httpAPIPublic = new Bool("httpAPIPublic", false)
			.describe("If false, the HTTP API effectively doesn't exist for anyone except localhost.");

	/**
	 * Request abuse prevention
	 */
	public final Bool httpRequestsEnabled = new Bool("httpRequestsEnabled", true)
			.describe("Allows disabling the whole HTTP business in the event of an emergency.");

	/**
	 * Request abuse prevention
	 */
	public final Int httpRequestTime = new Int("httpRequestTime", 30)
			.describe("During an HTTP request, amount of seconds the request may go on for before we decide we're being trolled.");

	/**
	 * Request abuse prevention
	 */
	public final Int httpRequestFakeLingerTime = new Int("httpRequestFakeLingerTime", 10)
			.describe("During an HTTP request, amount of seconds for a connection to linger for transmitting data back to the client.");

	/**
	 * Request abuse prevention
	 */
	public final Int httpRequestMaxLength = new Int("httpRequestMaxLength", 8192)
			.describe("During an HTTP request, maximum size of the header.");

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
