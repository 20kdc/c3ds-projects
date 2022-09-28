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
import natsue.data.hli.StandardMessages;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.system.SystemUserHubClient;
import natsue.server.userdata.INatsueUserData;

/**
 * This firewall module ensures that PRAY files with MESG and warp blocks are spooled if necessary.
 */
public class SpoolListFWModule implements IFWModule {
	public final IHubPrivilegedAPI hub;
	public final HashSet<String> spoolableBlocks = new HashSet<>();

	public SpoolListFWModule(IHubPrivilegedAPI h) {
		hub = h;
		// Blocks that imply spooling (this is so chat requests don't get spooled, which would be extremely dumb)
		spoolableBlocks.add("MESG"); // Message centre
		spoolableBlocks.add("warp"); // Warped creature
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public boolean handleMessage(INatsueUserData sourceUser, INatsueUserData destUser, PackedMessage message) {
		if (message instanceof PackedMessagePRAY) {
			PackedMessagePRAY pray = (PackedMessagePRAY) message;
			for (PRAYBlock block : pray.messageBlocks) {
				if (spoolableBlocks.contains(block.getType())) {
					hub.sendMessage(destUser, message, MsgSendType.Perm);
					return true;
				}
			}
		}
		return false;
	}
}
