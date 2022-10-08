/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.system.cmd;

import natsue.data.hli.ChatColours;
import natsue.server.system.SystemUserHubClient;

/**
 * Help set bot command
 */
public class HelpSetBotCommand extends BaseBotCommand {

	public HelpSetBotCommand(boolean isAdmin) {
		super(isAdmin ? "ahelp" : "help", "[command]",
				isAdmin ?
					"Describes admin commands" :
					"Describes commands",
				isAdmin ?
					"Helpful information on administrative commands.\nSpecifying a command gives more information on it." :
					"Helpful information on commands.\nSpecifying a command gives more information on it.",
				"help help",
				isAdmin ? Cat.Admin : Cat.Public);
	}

	@Override
	public void run(Context args) {
		String cmd = args.nextArg();
		if (cmd != null) {
			if (args.remaining()) {
				args.response.append("You can only lookup help for one command at a time.\n");
				return;
			}
			for (BaseBotCommand bbc : args.helpInfo) {
				if (bbc.name.equals(cmd) && bbc.category == category) {
					args.response.append(bbc.name);
					args.response.append(" ");
					args.response.append(bbc.helpArgs);
					args.response.append("\n");
					args.response.append(bbc.helpText);
					args.response.append("\n");
					args.response.append("Example: ");
					args.response.append("<tint 255 255 255>");
					args.response.append(bbc.name);
					args.response.append(" ");
					args.response.append(bbc.helpExample);
					args.response.append(ChatColours.CHAT);
					args.response.append("\n");
					return;
				}
			}
			args.response.append("Commad does not exist or is not covered by this category: ");
			args.response.append(cmd);
			args.response.append("\n");
		} else {
			args.response.append("Commands:\n");
			for (BaseBotCommand bbc : args.helpInfo) {
				if (bbc.category == category) {
					args.response.append("<tint 255 255 255>");
					args.response.append(bbc.name);
					args.response.append(ChatColours.CHAT);
					args.response.append(": ");
					args.response.append(bbc.helpSummary);
					args.response.append("\n");
				}
			}
			if (category == Cat.Admin) {
				args.response.append("You can send a global system message by mail, subject \"SYSTEM MSG\".\n");
			} else {
				if (args.hub.isUINAdmin(args.senderUIN)) {
					args.response.append("ahelp: for admin commands!\n");
				} else {
					args.response.append("Remember that you can use the third button on the right (Window sizing) to expand the chat panel.\n");
				}
			}
		}
	}

}
