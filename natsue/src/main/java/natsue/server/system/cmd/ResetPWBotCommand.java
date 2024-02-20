/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system.cmd;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import natsue.server.userdata.INatsueUserData;

/**
 * Resets a user's password.
 */
public class ResetPWBotCommand extends BaseBotCommand {
	public ResetPWBotCommand() {
		super("resetpw", "<user>",
				"Reset password",
				"Resets the password of a user, and reports it to you (so you can then tell them).",
				"Bubblespone",
				Cat.Admin2FA);
	}

	@Override
	public void run(Context args) {
		if (!args.remaining()) {
			args.response.append("Reset whose password?\n");
			return;
		}
		String user = args.nextArg();
		if (args.remaining()) {
			args.response.append("Only resets one password at a time.\n");
			return;
		}
		try (INatsueUserData.LongTermPrivileged userData = args.commandLookupUserLongTerm(user)) {
			if (userData != null) {
				String newPW;
				try {
					newPW = Long.toHexString(SecureRandom.getInstanceStrong().nextLong() | 0x8000000000000000L);
					if (userData.setPassword(newPW)) {
						args.appendNewPassword(newPW, userData);
					} else {
						args.response.append("Failed (not a normal user?)\n");
					}
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
			} else {
				args.appendNoSuchUser(user);
			}
		}
	}
}
