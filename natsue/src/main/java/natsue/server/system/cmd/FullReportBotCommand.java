/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.system.cmd;

import java.nio.charset.StandardCharsets;

import cdsp.common.data.pray.PRAYBlock;
import natsue.data.babel.PacketReader;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.hli.StandardMessages;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;

/**
 * Checks for oddities
 */
public class FullReportBotCommand extends BaseBotCommand {
	public FullReportBotCommand() {
		super("fullreportfile", "",
				"Debugging",
				"Dumps absolutely all details to a PRAY file you will receive.",
				"",
				Cat.Admin);
	}

	@Override
	public void run(Context args) {
		byte[] data = args.hub.runSystemCheck(true).getBytes(StandardCharsets.UTF_8);
		PRAYBlock pray = new PRAYBlock("INVI", StandardMessages.generateBlockName("INVI") + ".txt", data, PacketReader.CHARSET);
		PackedMessage pm = new PackedMessagePRAY(args.senderUIN, pray);
		args.hub.sendMessage(args.senderUIN, pm, MsgSendType.Temp, args.senderUIN);
		args.response.append("Check your Warp In directory!");
	}
}
