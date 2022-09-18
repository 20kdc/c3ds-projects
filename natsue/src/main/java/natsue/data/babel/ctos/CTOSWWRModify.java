/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel.ctos;

import natsue.data.babel.UINUtils;

public class CTOSWWRModify extends TargetUIDCTOS {
	public final boolean add;

	public CTOSWWRModify(boolean a) {
		add = a;
	}

	@Override
	public String toString() {
		return "CTOSWWRModify{of: " + UINUtils.toString(targetUIN) + ", add: " + add + "}";
	}

	@Override
	public int transactionDummyLength() {
		return 0;
	}
}
