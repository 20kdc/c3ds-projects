/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import java.util.HashSet;

import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.hubapi.INatsueUserData;

/**
 * A more complex firewall that analyzes messages to ensure they won't do anything "unusual" to clients.
 */
public class ComplexFirewall implements IFirewall, ILogSource {
	public final IHubPrivilegedAPI hub;
	public final HashSet<String> knownBlocks = new HashSet<>();
	public final HashSet<String> obviouslyDangerousBlocks = new HashSet<>();
	public final HashSet<String> spoolableBlocks = new HashSet<>();
	public final boolean restrictCustomBlocks;
	private final ILogProvider parentLog;

	public ComplexFirewall(ILogProvider pl, IHubPrivilegedAPI h, boolean noCustomBlocks) {
		hub = h;
		parentLog = pl;
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
		// Blocks that imply spooling (this is so chat requests don't get spooled, which would be extremely dumb)
		spoolableBlocks.add("MESG"); // Message centre
		spoolableBlocks.add("warp"); // Warped creature
	}

	@Override
	public ILogProvider getLogParent() {
		return parentLog;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public void handleMessage(INatsueUserData sourceUser, long destinationUIN, PackedMessage message) {
		message.senderUIN = sourceUser.getUIN();
		INatsueUserData destUserInfo = hub.getUserDataByUIN(destinationUIN);
		boolean temp = false;
		String rejection = null;
		try {
			if (destUserInfo == null) {
				rejection = "Destination non-existent";
			} else if (destUserInfo.isFrozen()) {
				rejection = "Destination frozen";
			} else if (message instanceof PackedMessagePRAY) {
				temp = true;
				PackedMessagePRAY pray = (PackedMessagePRAY) message;
				for (PRAYBlock block : pray.messageBlocks) {
					String type = block.getType();
					if (obviouslyDangerousBlocks.contains(type)) {
						rejection = "Blatantly dangerous PRAY block type: " + type;
						break;
					}
					if (restrictCustomBlocks && !knownBlocks.contains(type)) {
						rejection = "Custom PRAY block type: " + type;
						break;
					}
					if (spoolableBlocks.contains(type))
						temp = false;
					// Block-specific procedures
					if (type.equals("MESG")) {
						sanitizeMESG(sourceUser, block);
					} else if (type.equals("warp")) {
						// NB norn detector
						PRAYTags pt = new PRAYTags();
						pt.read(block.data);
						// not checking Genus right now - patch it when someone breaks it, things are on fire rn
						int reC = pt.intMap.get("Gender");
						boolean isNB = reC != 1 && reC != 2;
						if (isNB && !destUserInfo.isReceivingNBNorns()) {
							// NB norns crash people who aren't prepared to receive them.
							rejection = "NB norn that target couldn't receive";
							break;
						}
					}
				}
			}
		} catch (Exception ex2) {
			// oh no you DON'T
			log(ex2);
			hub.rejectMessage(destinationUIN, message, "Firewall threw exception");
			return;
		}
		if (rejection != null) {
			hub.rejectMessage(destinationUIN, message, rejection);
		} else {
			hub.sendMessage(destinationUIN, message, temp ? MsgSendType.Temp : MsgSendType.Perm);
		}
	}

	/**
	 * Sanitizes a MESG block to make sure the sender isn't faked.
	 */
	public void sanitizeMESG(INatsueUserData sourceUser, PRAYBlock mesgBlock) {
		PRAYTags pt = new PRAYTags();
		pt.read(mesgBlock.data);
		pt.strMap.put("Sender UserID", sourceUser.getUINString());
		pt.strMap.put("Sender Nickname", sourceUser.getNickName());
		mesgBlock.data = pt.toByteArray();
	}
}
