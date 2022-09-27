/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system.cmd;

import natsue.server.userdata.INatsueUserData;

/**
 * Controls flags.
 */
public class FlagControlBotCommand extends BaseBotCommand {
	public final String successMsg;
	public final int targetAnd;
	public final int targetXor;

	public FlagControlBotCommand(String cmd, String help, Cat category, int flag, boolean on, String success) {
		super(cmd, help, category);
		successMsg = success;
		targetAnd = ~flag;
		targetXor = on ? flag : 0;
	}

	@Override
	public void run(Context args) {
		try (INatsueUserData.LongTermPrivileged ltp = args.hub.openUserDataByUINLT(args.senderUIN)) {
			if ((ltp != null) && ltp.updateFlags(targetAnd, targetXor)) {
				args.hub.considerRandomStatus(ltp);
				args.response.append(successMsg);
			} else {
				args.response.append("Failed.\n");
			}
		}
	}
}
