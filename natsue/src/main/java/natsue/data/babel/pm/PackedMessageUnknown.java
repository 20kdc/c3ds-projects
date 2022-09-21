/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel.pm;

/**
 * PackedMessage of an unknown type!
 */
public class PackedMessageUnknown extends PackedMessage {
	public byte[] messageData;

	public PackedMessageUnknown(int type) {
		super(type);
	}

	public PackedMessageUnknown(long uin, int t) {
		super(uin, t);
	}

	public PackedMessageUnknown(long uin, int t, byte[] data) {
		super(uin, t);
		messageData = data;
	}

	@Override
	public byte[] getOrPackContents() {
		return messageData;
	}
}
