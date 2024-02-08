/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system;

import natsue.data.babel.UINUtils;
import natsue.data.hli.ChatColours;
import natsue.server.database.INatsueUserFlags;
import natsue.server.system.cmd.*;
import natsue.server.system.cmd.BaseBotCommand.Cat;
import natsue.server.userdata.INatsueUserData;

/**
 * Stuff them all here for now as part of refactor
 */
public class SystemCommands {
	public static final String VERSION = "Natsue Indev 08/02/24";
	public static final String VERSION_URL = "https://github.com/20kdc/c3ds-projects/tree/main/natsue";

	public static final BaseBotCommand[] commands = new BaseBotCommand[] {
		new HelpSetBotCommand(false),
		new HelpSetBotCommand(true),
		new BaseBotCommand("version", "",
				"Server version", "Shows the Natsue server software version.", "", Cat.Public) {
			public void run(Context args) {
				args.response.append(VERSION);
				args.response.append("\n");
			}
		},
		new WhoisBotCommand(),
		new BaseBotCommand("kick", "<user>",
				"Kicks a user", "Kicks (i.e. forcibly disconnects) a user from the server.", "kick JeremyRandomTroll", Cat.Admin) {
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
		new BaseBotCommand("setpw", "<password>",
				"Change password", "Updates your password.\nBe aware that this won't update your client's copy of the password, so you'll need to update it on next login.", "FirePoker123", Cat.Public) {
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
		new BaseBotCommand("who", "",
				"List users", "Lists all users connected to the server.", "", Cat.Public) {
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
		new FlagControlBotCommand("allownbnorns", "Allow NB norns", "Allows receipt of NB norns. May subject you to crashes if your world is not properly patched.", Cat.Public, INatsueUserFlags.FLAG_RECEIVE_NB_NORNS, true, "NB norn receipt enabled\n"),
		new FlagControlBotCommand("denynbnorns", "No NB norns", "Prevents receipt of NB norns.", Cat.Public, INatsueUserFlags.FLAG_RECEIVE_NB_NORNS, false, "NB norn receipt disabled\n"),
		new FlagControlBotCommand("allowgeats", "Allow geats", "Allows receipt of geats. May subject you to the Wasteland Glitch if your world is not properly patched.", Cat.Public, INatsueUserFlags.FLAG_RECEIVE_GEATS, true, "Geat receipt enabled\n"),
		new FlagControlBotCommand("denygeats", "No geats", "Prevents receipt of geats.", Cat.Public, INatsueUserFlags.FLAG_RECEIVE_GEATS, false, "Geat receipt disabled\n"),
		// note the inverse in the test for DB compat.
		new FlagControlBotCommand("allowrandom", "Allow random Norns", "Allows you to be targeted by the 'any on-line user' setting.", Cat.Public, INatsueUserFlags.FLAG_NO_RANDOM, false, "Random selection enabled\n"),
		new FlagControlBotCommand("denyrandom", "No random Norns", "Prevents being targeted by the 'any on-line user' setting.", Cat.Public, INatsueUserFlags.FLAG_NO_RANDOM, true, "Random selection disabled\n"),
		new BaseBotCommand("kickme", "",
				"Kicks you", "Kicks you.", "", Cat.Admin) {
			public void run(Context args) {
				args.hub.forceDisconnectUIN(args.senderUIN, false);
			}
		},
		new BaseBotCommand("whoami", "",
				"funny", "Funny joke command", "", Cat.Secret) {
			public void run(Context args) {
				args.response.append("A philosophical question.\n");
				args.response.append("To me? You are " + UINUtils.toString(args.senderUIN) + ".\n");
				args.response.append("Others may say differently.\n");
			}
		},
		new RemoteFlagControlBotCommand(),
		new SystemCheckBotCommand(),
		new FullReportBotCommand(),
		new DefrostBotCommand(),
		new CryoGetBotCommand()
	};
}
