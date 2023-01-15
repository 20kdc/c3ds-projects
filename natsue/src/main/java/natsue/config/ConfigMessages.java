/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

/**
 * Data formats / messages
 */
public class ConfigMessages extends BaseConfig.Group {
	/**
	 * PRAY chunk compression
	 */
	public final Bool compressPRAYChunks = new Bool("compressPRAYChunks", true)
			.describe("Compresses PRAY chunks. Natsue always decompresses them, but this enables recompressing them.");

	/**
	 * Maximum length of a creature name.
	 * Default chosen by the amount of the letter 'i' you can put in a name, with some rounding up.
	 */
	public final Int maxCreatureNameLen = new Int("maxCreatureNameLen", 32)
			.describe("Maximum length of a creature name.");

	/**
	 * Maximum length of a creature's user text.
	 * Default chosen by the amount of the letter 'i' you can put in the user text, with some rounding up.
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
}
