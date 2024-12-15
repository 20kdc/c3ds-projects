/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import cdsp.common.data.pray.ExportedCreatures;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.server.cryo.CryoFunctions;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.userdata.INatsueUserData;

/**
 * Checks that sent creatures are all okay.
 */
public class CreatureCheckingFWModule implements IFWModule {
	public final IHubPrivilegedAPI hub;

	public CreatureCheckingFWModule(IHubPrivilegedAPI h) {
		hub = h;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public boolean handleMessage(INatsueUserData sourceUser, INatsueUserData destUser, PackedMessage message) {
		if (message instanceof PackedMessagePRAY) {
			PackedMessagePRAY pray = (PackedMessagePRAY) message;
			if (CryoFunctions.expectedToContainACreature(pray.messageBlocks)) {
				// check that it's well-formed
				String err = ExportedCreatures.checkWellFormedCreature(pray.messageBlocks);
				if (err != null) {
					hub.rejectMessage(destUser.getUIN(), message, err);
					return true;
				}
				// and then check receipt is OK
				err = CryoFunctions.receiptCompatibilityCheck(pray.messageBlocks, destUser);
				if (err != null) {
					hub.rejectMessage(destUser.getUIN(), message, err);
					return true;
				}
			}
		}
		return false;
	}
}
