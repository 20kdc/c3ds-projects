/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

/**
 * Cryogenics
 */
public class ConfigCryo extends BaseConfig.Group {
	/**
	 * Cryogenics quota
	 */
	public final Int cryoQuotaBytes = new Int("cryoQuotaBytes", 0x10000000)
			.describe("Total cryo quota in bytes before we stop accepting data. Default is 256MB, maximum is 2GB.");

	/**
	 * Cryogenics submission enabled
	 */
	public final Bool cryoSubmitEnabled = new Bool("cryoSubmitEnabled", true)
			.describe("Cryo submission on/off, allows storage of creatures in System.");

	/**
	 * Cryogenics submit enabled for non-admins
	 */
	public final Bool cryoSubmitPublic = new Bool("cryoSubmitPublic", true)
			.describe("Cryo submission for non-admins.");

	/**
	 * Cryogenics dir.
	 */
	public final Str cryoDir = new Str("cryoDir", "cryo")
			.describe("Cryo directory, creature files are written and read from here.");
}
