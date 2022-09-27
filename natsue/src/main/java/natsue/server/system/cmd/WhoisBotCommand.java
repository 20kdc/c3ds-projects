/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system.cmd;

import natsue.data.babel.UINUtils;
import natsue.data.hli.ChatColours;
import natsue.server.database.INatsueUserFlags;
import natsue.server.userdata.INatsueUserData;

/**
 * More information on people
 */
public class WhoisBotCommand extends BaseBotCommand {
	public WhoisBotCommand() {
		super("whois", "<users...>", Cat.Public);
	}

	@Override
	public void run(Context args) {
		while (args.remaining()) {
			String user = args.nextArg();
			INatsueUserData userData = args.commandLookupUser(user);
			if (userData != null) {
				args.response.append(ChatColours.NICKNAME);
				args.response.append(userData.getNickname());
				args.response.append(ChatColours.CHAT);
				args.response.append(" - ");
				if (args.hub.isUINOnline(userData.getUIN())) {
					args.response.append("<tint 64 255 64>Online\n");
				} else {
					args.response.append("<tint 255 64 64>Offline\n");
				}
				args.response.append(ChatColours.CHAT);
				args.response.append("UIN: ");
				args.response.append(userData.getUINString());
				int hid = UINUtils.hid(userData.getUIN()); 
				if (hid == UINUtils.HID_SYSTEM) {
					args.response.append(" <tint 64 64 255>(SYSTEM)\n");
				} else if (hid == UINUtils.HID_USER) {
					args.response.append(" <tint 64 255 64>(USER)\n");
				} else {
					args.response.append(" <tint 255 64 64>(" + hid + ")\n");
				}
				args.response.append(ChatColours.CHAT);
				args.response.append("Flags: ");
				args.response.append(INatsueUserFlags.Flag.showFlags(userData.getFlags()));
				args.response.append("\n");
			} else {
				args.appendNoSuchUser(user);
			}
		}
	}
}
