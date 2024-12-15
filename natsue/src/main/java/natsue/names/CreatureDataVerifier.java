/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.names;

import cdsp.common.data.Monikers;
import natsue.config.ConfigMessages;

/**
 * Used to also verify monikers, that got moved to common
 */
public class CreatureDataVerifier {
	private static String verifyLimitLen(int l, String name) {
		if (name.length() > l)
			name = name.substring(0, l);
		return name;
	}

	private static String verifyHumanReadableLen(int l, String name, boolean newLines) {
		return verifyLimitLen(l, name);
	}

	/**
	 * Verifies / strips a name.
	 */
	public static String stripMonikerLike(String name) {
		return verifyLimitLen(Monikers.MAX_MONIKER_LEN, name);
	}

	/**
	 * Verifies / strips a name.
	 */
	public static String stripName(ConfigMessages config, String name) {
		return verifyHumanReadableLen(config.maxCreatureNameLen.getValue(), name, false);
	}

	/**
	 * Verifies / strips user text.
	 */
	public static String stripUserText(ConfigMessages config, String userText) {
		return verifyHumanReadableLen(config.maxCreatureUserTextLen.getValue(), userText, true);
	}
}
