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
public class ConfigPhotos extends BaseConfig.Group {
	/**
	 * Photo on/off
	 */
	public final Bool photosEnabled = new Bool("photosEnabled", true)
			.describe("Enables/disables photo storage.");

	/**
	 * Log photo errors
	 */
	public final Bool logPhotoErrors = new Bool("logPhotoErrors", true)
			.describe("Log photo errors.");
}
