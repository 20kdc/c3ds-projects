/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.config;

/**
 * Configuration for quotas
 */
public class ConfigConnectionQuotas extends BaseConfig.Group {
	/**
	 * Max. connections in two minutes.
	 */
	public final Int maxConnectionsInTwoMinutes = new Int("quotaMaxConnectionsInTwoMinutes", 60)
			.describe("Maximum amount of new connections in two minutes per IP address.");
	/**
	 * Max. connections in two minutes.
	 */
	public final Int maxConnectionsConcurrent = new Int("quotaMaxConnectionsConcurrent", 16)
			.describe("Maximum amount of concurrent connections per IP address.");
}
