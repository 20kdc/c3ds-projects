/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import rals.diag.SrcPosUntranslated;

/**
 * Storage of HCM data (since HCM data on all active LSP files remains in memory at all times)
 */
public class HCMStorage {

	/**
	 * Gets hover information at a given point (or null for none)
	 */
	public HoverData getHoverData(SrcPosUntranslated tkn) {
		return null;
	}

	public static final class HoverData {
		public final String text;
		public HoverData(String t) {
			text = t;
		}
	}
}
