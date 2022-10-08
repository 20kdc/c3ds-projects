/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.system.cmd;

import natsue.data.babel.UINUtils;
import natsue.server.database.INatsueUserFlags;
import natsue.server.database.INatsueUserFlags.Flag;
import natsue.server.userdata.INatsueUserData;

/**
 * General flag manipulation command
 */
public class RemoteFlagControlBotCommand extends BaseBotCommand {
	public RemoteFlagControlBotCommand() {
		super("flags", "[USER [+FLAG/-FLAG]...]",
				"Alter flags",
				"If no parameters are provided, shows a reference of all flags and their meanings.\n" +
				"If a user is provided, shows/updates their flags.\n" +
				"'+' prefixed entries add that flag, '-' prefixed entries remove it.\n" +
				"+FROZEN is best paired with the kick command for maximum effect.",
				"BadUser +FROZEN",
				Cat.Admin);
	}

	@Override
	public void run(Context args) {
		if (args.remaining()) {
			String user = args.nextArg();
			int set = 0;
			int unset = 0;
			while (true) {
				String flag = args.nextArg();
				if (flag == null)
					break;
				boolean add;
				if (flag.startsWith("+")) {
					add = true;
				} else if (flag.startsWith("-")) {
					add = false;
				} else {
					args.response.append("Flag specifier must have + or -: ");
					args.response.append(flag);
					args.response.append("\n");
					return;
				}
				flag = flag.substring(1);
				Flag fObj;
				try {
					fObj = Flag.valueOf(flag.toLowerCase());
				} catch (IllegalArgumentException iae) {
					args.response.append("Unknown flag: ");
					args.response.append(flag);
					args.response.append("\n");
					return;
				}
				if (add) {
					set |= fObj.value;
				} else {
					unset |= fObj.value;
				}
			}
			int and = ~(set | unset);
			int xor = set;
			try (INatsueUserData.LongTermPrivileged ltp = args.commandLookupUserLongTerm(user)) {
				if (ltp == null) {
					args.appendNoSuchUser(user);
				} else if (!ltp.updateFlags(and, xor)) {
					args.response.append("Failed to update flags, system user?\n");
				} else {
					args.hub.considerRandomStatus(ltp);
					String flagsInfo = INatsueUserFlags.Flag.showFlags(ltp.getFlags());
					String setInfo = INatsueUserFlags.Flag.showFlags(set);
					String unsetInfo = INatsueUserFlags.Flag.showFlags(unset);
					args.log.log(UINUtils.toString(args.senderUIN) + " changed " + user + " flags, set: " + setInfo + " unset: " + unsetInfo);
					args.response.append("Flags updated to: ");
					args.response.append(flagsInfo);
					args.response.append("\n");
				}
			}
		} else {
			args.response.append("FLAGS REFERENCE:\n");
			for (Flag f : INatsueUserFlags.Flag.values()) {
				args.response.append(f.name().toUpperCase());
				args.response.append(": ");
				args.response.append(f.detail);
				args.response.append("\n");
			}
		}
	}
	
}
