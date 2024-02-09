/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.system.cmd;

import java.nio.ByteBuffer;

import cdsp.common.data.IOUtils;
import natsue.data.babel.PacketWriter;
import natsue.data.babel.UINUtils;
import natsue.server.session.ISessionClient;

/**
 * Test sequence.
 */
public class GetVCHandshakeResponseBotCommand extends BaseBotCommand {

	public GetVCHandshakeResponseBotCommand() {
		super("gvchr", "", "Test sequence",
				"Test sequence. Mostly harmless to server, but client may be unhappy!",
				"gvchr",
				Cat.Research);
	}

	@Override
	public void run(Context args) {
		if (args.remaining()) {
			args.response.append("Too many args.\n");
			return;
		}
		ISessionClient escalator = args.hub.acquireSessionClientForResearchCommands(args.senderUIN);
		try {
			long vcrUIN = UINUtils.VC_RESEARCH_UIN;
			escalator.sendPacket(PacketWriter.writeVirtualConnect(vcrUIN, (short) 1));
			Thread.sleep(2000);
			byte[] data = new byte[12];
			ByteBuffer bb = IOUtils.wrapLE(data);
			bb.putInt(0, 1);
			bb.putInt(4, UINUtils.uid(vcrUIN));
			bb.putInt(8, UINUtils.hid(vcrUIN));
			escalator.sendPacket(PacketWriter.writeVirtualCircuitData(vcrUIN, (short) 1, args.senderUIN, (short) 1, data));
		} catch (Exception ex) {
			args.log.log(ex);
		}
		args.response.append("Done.\n");
	}

}
