/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system.cmd;

import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.server.cryo.CryoFrontend.LockedFN;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.system.SystemUserHubClient;
import natsue.server.userdata.INatsueUserData;

/**
 * Retrieves a file from cryo.
 */
public class DefrostBotCommand extends BaseBotCommand {
	public DefrostBotCommand() {
		super("defrost", "",
				"Grabs a random creature",
				"Retrieves a random creature from cryogenic storage (i.e. creatures sent to !System).",
				"",
				Cat.Public);
	}

	@Override
	public void run(Context args) {
		if (args.remaining()) {
			args.response.append("No, you can't pick one.\n");
			return;
		}
		INatsueUserData sender = args.hub.getUserDataByUIN(args.senderUIN);
		if (sender == null) {
			args.response.append("How'd this happen? You don't exist.\n");
			return;
		}
		try (LockedFN lfn = args.hub.getCryoFE().getRandomFNAndLock(sender)) {
			if (lfn != null) {
				// Yes, creatures we defrost get put in the permanent transmission buffer.
				// The alternative is an interesting issue if we're very very close to quota...
				args.hub.sendMessage(sender, new PackedMessagePRAY(SystemUserHubClient.IDENTITY.getUIN(), lfn.content), MsgSendType.Perm, sender);
				lfn.doDelete();
				if (sender.isAdmin()) {
					args.response.append("Defrosting: " + lfn.fn + "\n");
				} else {
					args.response.append("Defrosting, please wait...\n");
				}
			} else {
				args.response.append("No compatible creatures in cryogenic storage.\n");
			}
		}
	}
}
