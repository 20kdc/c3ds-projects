/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.hli.StandardMessages;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.system.SystemUserHubClient;
import natsue.server.userdata.INatsueUserData;

/**
 * A more complex firewall that analyzes messages to ensure they won't do anything "unusual" to clients.
 */
public class ComplexFWModule implements IFWModule {
	public final IHubPrivilegedAPI hub;

	public ComplexFWModule(IHubPrivilegedAPI h) {
		hub = h;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public boolean handleMessage(INatsueUserData sourceUser, INatsueUserData destUser, PackedMessage message) {
		if (message instanceof PackedMessagePRAY) {
			PackedMessagePRAY pray = (PackedMessagePRAY) message;
			for (PRAYBlock block : pray.messageBlocks) {
				String type = block.getType();
				// Block-specific procedures
				if (type.equals("MESG")) {
					sanitizeMESG(sourceUser, block);
				} else if (type.equals("REQU")) {
					PRAYTags pt = new PRAYTags();
					pt.read(block.data);
					String chatID = pt.strMap.getOrDefault("ChatID", "?");
					String reqType = pt.strMap.getOrDefault("Request Type", "?");
					if (chatID.equals(SystemUserHubClient.CHATID_GLOBAL) && !reqType.equals("Accept")) {
						// This request is forfeit as it would break the global chat system
						PackedMessage pm = StandardMessages.systemMessage(message.senderUIN, "You can't invite people to Global Chat.");
						hub.sendMessage(message.senderUIN, pm, MsgSendType.Temp, message.senderUIN);
						return true;
					}
				} else if (type.equals("warp")) {
					// NB norn detector
					PRAYTags pt = new PRAYTags();
					pt.read(block.data);
					// not checking Genus right now - patch it when someone breaks it, things are on fire rn
					int reC = pt.intMap.get("Gender");
					int reG = pt.intMap.get("Genus");
					boolean isNB = reC != 1 && reC != 2;
					boolean isGeat = reG == 4;
					if (reG < 1 || reG > 4) {
						hub.rejectMessage(destUser.getUIN(), message, "Invalid creature genus");
						return true;
					} else if (isGeat && !destUser.isReceivingGeats()) {
						// Wasteland glitch prevention (CACL 4 4 0 19!!!!)
						hub.rejectMessage(destUser.getUIN(), message, "Geat that target couldn't receive");
						return true;
					} else if (isNB && !destUser.isReceivingNBNorns()) {
						// NB norns crash people who aren't prepared to receive them.
						hub.rejectMessage(destUser.getUIN(), message, "NB norn that target couldn't receive");
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Sanitizes a MESG block to make sure the sender isn't faked.
	 */
	public void sanitizeMESG(INatsueUserData sourceUser, PRAYBlock mesgBlock) {
		PRAYTags pt = new PRAYTags();
		pt.read(mesgBlock.data);
		pt.strMap.put("Sender UserID", sourceUser.getUINString());
		pt.strMap.put("Sender Nickname", sourceUser.getNickname());
		mesgBlock.data = pt.toByteArray();
	}
}
