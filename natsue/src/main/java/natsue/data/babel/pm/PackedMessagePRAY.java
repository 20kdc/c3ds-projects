/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel.pm;

import java.util.LinkedList;

import cdsp.common.data.pray.PRAYBlock;

/**
 * PRAY PackedMessage!
 */
public class PackedMessagePRAY extends PackedMessage {
	public LinkedList<PRAYBlock> messageBlocks;

	public PackedMessagePRAY() {
		super(TYPE_PRAY);
	}

	public PackedMessagePRAY(long uin) {
		super(uin, TYPE_PRAY);
	}

	public PackedMessagePRAY(long uin, LinkedList<PRAYBlock> data) {
		super(uin, TYPE_PRAY);
		messageBlocks = data;
	}

	public PackedMessagePRAY(long uin, PRAYBlock data) {
		super(uin, TYPE_PRAY);
		messageBlocks = new LinkedList<>();
		messageBlocks.add(data);
	}

	@Override
	public byte[] getOrPackContents(boolean compress) {
		return PRAYBlock.write(messageBlocks, compress);
	}
}
