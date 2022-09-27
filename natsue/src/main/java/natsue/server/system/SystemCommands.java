/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system;

import natsue.data.babel.UINUtils;
import natsue.data.hli.ChatColours;
import natsue.data.hli.StandardMessages;
import natsue.server.database.INatsueUserFlags;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.system.cmd.*;
import natsue.server.system.cmd.BaseBotCommand.Cat;
import natsue.server.userdata.INatsueUserData;

/**
 * Stuff them all here for now as part of refactor
 */
public class SystemCommands {
	public static final BaseBotCommand[] commands = new BaseBotCommand[] {
		new WhoisBotCommand(),
		new BaseBotCommand("contact", "<users...>", Cat.Public) {
			public void run(Context args) {
				while (args.remaining()) {
					String user = args.nextArg();
					INatsueUserData userData = args.commandLookupUser(user);
					if (userData != null) {
						args.response.append("Adding ");
						args.response.append(ChatColours.NICKNAME);
						args.response.append(userData.getNickname());
						args.response.append(ChatColours.CHAT);
						args.response.append(" to your contact list...\n");
						args.hub.sendMessage(args.senderUIN, StandardMessages.addToContactList(args.senderUIN, userData.getUIN()), MsgSendType.Temp);
					} else {
						args.appendNoSuchUser(user);
					}
				}
			}
		},
		new BaseBotCommand("kick", "<user>", Cat.Admin) {
			public void run(Context args) {
				if (!args.remaining()) {
					args.response.append("Kick who?\n");
					return;
				}
				String user = args.nextArg();
				if (args.remaining()) {
					args.response.append("Only kicks one user at a time.\n");
					return;
				}
				INatsueUserData userData = args.commandLookupUser(user);
				if (userData != null) {
					args.response.append("Kicking (if online)\n");
					args.hub.forceDisconnectUIN(userData.getUIN(), false);
				} else {
					args.appendNoSuchUser(user);
				}
			}
		},
		new ResetPWBotCommand(),
		new BaseBotCommand("setpw", "<password>", Cat.Public) {
			public void run(Context args) {
				if (!args.remaining()) {
					args.response.append("New password cannot be empty.\n");
					return;
				}
				String newPW = args.toEnd();
				try (INatsueUserData.LongTermPrivileged ltp = args.hub.openUserDataByUINLT(args.senderUIN)) {
					if ((ltp != null) && ltp.setPassword(newPW)) {
						args.response.append("Reset password to: " + newPW + "\n");
					} else {
						args.response.append("Failed (not a normal user?)\n");
					}
				}
			}
		},
		new BaseBotCommand("who", "", Cat.Public) {
			public void run(Context args) {
				if (args.remaining()) {
					args.response.append("No parameters, please.\n");
					return;
				}
				boolean first = true;
				for (INatsueUserData data : args.hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
					if (!first) {
						args.response.append(ChatColours.CHAT);
						args.response.append(", ");
					}
					args.response.append(ChatColours.NICKNAME);
					args.response.append(data.getNickname());
					first = false;
				}
				args.response.append("\n");
			}
		},
		new FlagControlBotCommand("allownbnorns", "", Cat.Public, INatsueUserFlags.FLAG_RECEIVE_NB_NORNS, true, "NB norn receipt enabled\n"),
		new FlagControlBotCommand("denynbnorns", "", Cat.Public, INatsueUserFlags.FLAG_RECEIVE_NB_NORNS, false, "NB norn receipt disabled\n"),
		new BaseBotCommand("kickme", "", Cat.Public) {
			public void run(Context args) {
				args.hub.forceDisconnectUIN(args.senderUIN, false);
			}
		},
		new BaseBotCommand("whoami", "", Cat.Secret) {
			public void run(Context args) {
				args.response.append("A philosophical question.\n");
				args.response.append("To me? You are " + UINUtils.toString(args.senderUIN) + ".\n");
				args.response.append("Others may say differently.\n");
			}
		},
		new BaseBotCommand("ahelp", "", Cat.Admin) {
			public void run(Context args) {
				args.response.append("admin commands:\n");
				args.response.append("kick Someone\n");
				args.response.append("resetpw Someone\n");
				args.response.append("You can send a global system message by mail, subject \"SYSTEM MSG\".\n");
			}
		},
		new BaseBotCommand("help", "", Cat.Admin) {
			public void run(Context args) {
				args.response.append("whois !System\n");
				args.response.append("contact !System\n");
				args.response.append("who (show who's online)\n");
				args.response.append("(allow/deny)nbnorns (WARNING: Crashes you if unmodded!)\n");
				args.response.append("setpw 1234 (sets your password)\n");
				if (args.hub.isUINAdmin(args.senderUIN))
					args.response.append("For admin tasks try: ahelp\n");
			}
		}
	};
}
