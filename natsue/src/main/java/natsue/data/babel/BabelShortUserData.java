/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.nio.ByteBuffer;

/**
 * The information on a user sent to the client.
 */
public class BabelShortUserData {
	/**
	 * Theoretically, someone's first name.
	 * In practice, don't send it - the client never uses this anyway, and it's not nice.
	 */
	public final String firstName;

	/**
	 * Theoretically, someone's last name.
	 * In practice, don't send it - the client never uses this anyway, and it's not nice.
	 */
	public final String lastName;

	/**
	 * The user's nickname.
	 * Note that this is the "unfolded" version.
	 */
	public final String nickname;

	/**
	 * The user's UIN - see {@link UINUtils}.
	 */
	public final long uin;

	/**
	 * A packed version of this BabelShortUserData (as these objects tend to stick around for long periods).
	 * Do not modify contents!
	 */
	public final byte[] packed;

	public BabelShortUserData(String f, String l, String n, long u) {
		firstName = f;
		lastName = l;
		nickname = n;
		uin = u;
		byte[] fn = firstName.getBytes(PacketReader.CHARSET);
		byte[] ln = lastName.getBytes(PacketReader.CHARSET);
		byte[] nn = nickname.getBytes(PacketReader.CHARSET);
		byte[] total = new byte[24 + fn.length + ln.length + nn.length];
		System.arraycopy(fn, 0, total, 24, fn.length);
		System.arraycopy(ln, 0, total, 24 + fn.length, ln.length);
		System.arraycopy(nn, 0, total, 24 + fn.length + ln.length, nn.length);
		ByteBuffer bb = PacketReader.wrapLE(total);
		bb.putInt(0, total.length);
		bb.putInt(4, UINUtils.uid(uin));
		bb.putInt(8, UINUtils.hid(uin));
		bb.putInt(12, fn.length);
		bb.putInt(16, ln.length);
		bb.putInt(20, nn.length);
		packed = total;
	}
}
