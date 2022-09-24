/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

/**
 * Configuration of allowed nickname format
 */
public class ConfigNicknameFormat extends BaseConfig.Group {
	/**
	 * Every allowed character in a nickname after case-folding.
	 */
	public final Str nicknameAllowedCharacters = new Str("nicknameAllowedCharacters", "0123456789_-abcdefghijklmnopqrstuvwxyz")
			.describe("Every allowed character in a nickname *after case-folding.*.");

	/**
	 * Minimum length of a nickname.
	 */
	public final Int nicknameMinLength = new Int("nicknameMinLength", 1)
			.describe("Minimum length of a nickname.");

	/**
	 * Maximum length of a nickname.
	 */
	public final Int nicknameMaxLength = new Int("nicknameMaxLength", 16)
			.describe("Maximum length of a nickname.");
}
