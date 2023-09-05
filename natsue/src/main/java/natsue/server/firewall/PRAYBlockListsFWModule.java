/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import java.util.HashSet;

import cdsp.common.data.pray.PRAYBlock;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.userdata.INatsueUserData;

/**
 * Implements varying levels of early blocking based on PRAY blocks.
 */
public class PRAYBlockListsFWModule implements IFWModule {
	public final IHubPrivilegedAPI hub;
	public final HashSet<String> knownBlocks = new HashSet<>();
	public final HashSet<String> obviouslyDangerousBlocks = new HashSet<>();
	public final boolean restrictCustomBlocks;

	public PRAYBlockListsFWModule(IHubPrivilegedAPI h, boolean noCustomBlocks) {
		hub = h;
		restrictCustomBlocks = noCustomBlocks;
		// Blocks that imply danger
		obviouslyDangerousBlocks.add("DSAG"); // Agent injector
		obviouslyDangerousBlocks.add("AGNT"); // Agent injector
		obviouslyDangerousBlocks.add("EGGS"); // Muco
		obviouslyDangerousBlocks.add("EXPC"); // C3 Import
		obviouslyDangerousBlocks.add("DSEX"); // DS Import
		obviouslyDangerousBlocks.add("SFAM"); // C3 Starter Family
		obviouslyDangerousBlocks.add("DFAM"); // DS Starter Family
		obviouslyDangerousBlocks.add("CHUM"); // Contact list
		obviouslyDangerousBlocks.add("FILE"); // Installable file
		// Known blocks that vanilla will send
		knownBlocks.add("REQU"); // chat requests
		knownBlocks.add("CHAT"); // chat
		knownBlocks.add("MESG"); // Message centre
		knownBlocks.add("warp"); // Warped creature
		knownBlocks.add("GLST"); // ...
		knownBlocks.add("CREA"); // ...
		knownBlocks.add("GENE"); // ...
		knownBlocks.add("PHOT"); // ...
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
				if (obviouslyDangerousBlocks.contains(type)) {
					hub.rejectMessage(destUser, message, "Blatantly dangerous PRAY block type: " + type);
					return true;
				}
				if (restrictCustomBlocks && !knownBlocks.contains(type)) {
					hub.rejectMessage(destUser, message, "Custom PRAY block type: " + type);
					return true;
				}
			}
		}
		return false;
	}
}
