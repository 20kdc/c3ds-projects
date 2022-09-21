/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import java.util.HashSet;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.pray.PRAYBlock;
import natsue.server.hubapi.IHubPrivilegedAPI;

/**
 * A more complex firewall that analyzes messages to ensure they won't do anything "unusual" to clients.
 */
public class ComplexFirewall implements IFirewall {
	public final IHubPrivilegedAPI hub;
	public final HashSet<String> obviouslyDangerousBlocks = new HashSet<>();
	public final HashSet<String> spoolableBlocks = new HashSet<>();

	public ComplexFirewall(IHubPrivilegedAPI h) {
		hub = h;
		// Blocks that imply danger
		obviouslyDangerousBlocks.add("DSAG"); // Agent injector
		obviouslyDangerousBlocks.add("AGNT"); // Agent injector
		obviouslyDangerousBlocks.add("EGGS"); // Muco
		obviouslyDangerousBlocks.add("EXPC"); // Import
		obviouslyDangerousBlocks.add("DSEX"); // Import
		obviouslyDangerousBlocks.add("DFAM"); // DS Starter Family
		obviouslyDangerousBlocks.add("CHUM"); // Contact list
		obviouslyDangerousBlocks.add("FILE"); // Installable file
		// Blocks that imply spooling (this is so chat requests don't get spooled, which would be extremely dumb)
		spoolableBlocks.add("MESG"); // Message centre
		spoolableBlocks.add("warp"); // Warped creature
	}

	@Override
	public void wwrNotify(boolean online, BabelShortUserData userData) {
	}

	@Override
	public void handleMessage(BabelShortUserData sourceUser, long destinationUIN, PackedMessage message) {
		if (message.senderUIN != sourceUser.uin)
			return;
		boolean temp = false;
		if (message instanceof PackedMessagePRAY) {
			temp = true;
			PackedMessagePRAY pray = (PackedMessagePRAY) message;
			for (PRAYBlock block : pray.messageBlocks) {
				String type = block.getType();
				if (obviouslyDangerousBlocks.contains(type))
					return;
				if (spoolableBlocks.contains(type))
					temp = false;
			}
		}
		hub.sendMessage(destinationUIN, message, temp);
	}
}
