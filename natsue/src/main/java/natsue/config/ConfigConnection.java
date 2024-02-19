/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.config;

/**
 * Configuration for incoming connections
 */
public class ConfigConnection extends BaseConfig.Group {
	/**
	 * The port number.
	 */
	public final Int port = new Int("port", 49152)
			.describe("TCP port on which the server should listen.");

	/**
	 * The port number.
	 */
	public final Int portSSL = new Int("portSSL", -1)
			.describe("TCP port on which the server should listen for SSL. -1 indicates the SSL server should not be started.");

	/**
	 * SSL full chain.
	 */
	public final Str sslFullChainPEM = new Str("sslFullChainPEM", "")
			.describe("SSL certificate chain file (concatenated PEM certificates).");

	/**
	 * SSL private key.
	 */
	public final Str sslPrivKeyPEM = new Str("sslPrivKeyPEM", "")
			.describe("SSL private key file");

	/**
	 * SSL private key algorithm.
	 */
	public final Str sslPrivKeyAlgorithm = new Str("sslPrivKeyAlgorithm", "")
			.describe("SSL private key algorithm (if blank, tries guessing)");
}
