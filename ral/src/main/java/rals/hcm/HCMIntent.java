/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.Map;

import rals.diag.SrcPosUntranslated;
import rals.hcm.HCMStorage.HoverData;
import rals.lex.Token;

/**
 * Indicates the intent of a token.
 */
public abstract class HCMIntent {
	/**
	 * Retrieves a map.
	 * DO NOT WRITE TO THIS MAP.
	 * The token given is the key in intentsOnNextToken, AND MAY BE NULL.
	 * The position given is the actual position of the cursor.
	 */
	public abstract Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated tp, HCMStorage storage);
}
