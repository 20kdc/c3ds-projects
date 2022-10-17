/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import java.util.LinkedList;

import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.userdata.INatsueUserData;

/**
 * The Rejector that's actually going to be used.
 */
public class Rejector implements IRejector {
	// probably !System
	private final INatsueUserData onBehalfOf;
	private final IHubPrivilegedAPI hub;

	public Rejector(IHubPrivilegedAPI privapi, INatsueUserData behalf) {
		hub = privapi;
		onBehalfOf = behalf;
	}

	@Override
	public void rejectMessage(long destinationUIN, PackedMessage message, String reason) {
		if (message instanceof PackedMessagePRAY) {
			// Try to classify the message
			LinkedList<PRAYBlock> blocks = ((PackedMessagePRAY) message).messageBlocks;
			for (PRAYBlock blk : blocks) {
				String lastType = blk.getType();
				if (lastType.equals("warp")) {
					rejectWarpedCreature(destinationUIN, message, reason, blk);
					return;
				} else if (lastType.equals("MESG")) {
					rejectMail(destinationUIN, message, reason, blk);
					return;
				}
			}
		}
	}

	private void rejectWarpedCreature(long destinationUIN, PackedMessage message, String reason, PRAYBlock last) {
		PRAYTags pt = new PRAYTags();
		pt.read(last.data);
		pt.strMap.put("Last Network User", UINUtils.toString(destinationUIN));
		pt.intMap.put("Pray Extra foe", 0);
		pt.intMap.put("Pray Extra reject", 4);
		last.data = pt.toByteArray();
		sendInversion(destinationUIN, message);
	}

	private void rejectMail(long destinationUIN, PackedMessage message, String reason, PRAYBlock last) {
		PRAYTags pt = new PRAYTags();
		pt.read(last.data);
		pt.strMap.put("Subject", "ERR: " + pt.strMap.get("Subject"));
		pt.strMap.put("Sender UserID", UINUtils.toString(onBehalfOf.getUIN()));
		pt.strMap.put("Sender Nickname", onBehalfOf.getNickname());
		pt.strMap.put("Message", "Unsendable to " + UINUtils.toString(destinationUIN) + " (" + reason + "), contents:\n" + pt.strMap.get("Message"));
		last.data = pt.toByteArray();
		sendAsSystem(message);
	}

	private void sendInversion(long destinationUIN, PackedMessage message) {
		long tmp = message.senderUIN;
		message.senderUIN = destinationUIN;
		hub.sendMessage(tmp, message, MsgSendType.PermReject, message.senderUIN);
	}

	private void sendAsSystem(PackedMessage message) {
		long tmp = message.senderUIN;
		message.senderUIN = onBehalfOf.getUIN();
		hub.sendMessage(tmp, message, MsgSendType.PermReject, message.senderUIN);
	}
}
