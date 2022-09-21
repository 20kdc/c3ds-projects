/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel.pm;

import natsue.data.babel.WritVal;

/**
 * PackedMessage of a NET: WRIT.
 */
public class PackedMessageWrit extends PackedMessage {
	public String channel;
	public int messageId;
	public Object param1;
	public Object param2;

	public PackedMessageWrit() {
		super(TYPE_WRIT);
	}

	public PackedMessageWrit(long uin) {
		super(uin, TYPE_WRIT);
	}

	public PackedMessageWrit(long uin, String c, int mid, Object p1, Object p2) {
		super(uin, TYPE_WRIT);
		channel = c;
		messageId = mid;
		param1 = p1;
		param2 = p2;
	}

	@Override
	public byte[] getOrPackContents() {
		return WritVal.encodeWrit(channel, messageId, param1, param2);
	}
}
