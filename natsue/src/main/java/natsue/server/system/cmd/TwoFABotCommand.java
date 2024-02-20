/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system.cmd;

import java.security.SecureRandom;

import natsue.data.TOTP;
import natsue.data.babel.UINUtils;
import natsue.names.PWHash;
import natsue.server.userdata.INatsueUserData;

/**
 * More information on people
 */
public class TwoFABotCommand extends BaseBotCommand {
	public TwoFABotCommand() {
		super("2fa", "(...)",
				"TOTP auth",
				"",
				"123456",
				Cat.Secret);
	}

	@Override
	public void run(Context args) {
		if (!args.remaining()) {
			args.response.append("Need TOTP code (the 6-digit number that changes every 30 seconds).\n");
			return;
		}
		String str = args.nextArg();
		// DO NOT DO THIS UNLESS YOU KNOW WHAT YOU ARE DOING
		if (str.equalsIgnoreCase("confirm_enable_2fa")) {
			if (!args.remaining()) {
				args.response.append("Password is required to setup 2FA\n");
				return;
			}
			String pw = args.toEnd();
			try (INatsueUserData.LongTermPrivileged userData = args.hub.openUserDataByUINLT(args.senderUIN)) {
				if (userData == null) {
					args.response.append("How do you not exist?\n");
					return;
				}
				if (userData.has2FAConfigured()) {
					args.response.append("You have already enabled 2FA\n");
					return;
				}
				if (!PWHash.verify(UINUtils.uid(userData.getUIN()), userData.getPasswordHash(), pw, true)) {
					args.response.append("Password incorrect\n");
					return;
				}
				long newVal;
				try {
					newVal = SecureRandom.getInstanceStrong().nextLong() | 1;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				byte[] secret = PWHash.make2FA(newVal, pw);
				String secretStr;
				try {
					secretStr = new String(TOTP.encodeBase32(secret));
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				args.response.append("2FA Secret: " + secretStr + "\n");
				args.response.append("This is a TOTP code.\n");
				args.response.append("You can pass it to the various phone authenticators.\n");
				args.response.append("Do not lose this!\n");
				if (!userData.update2FA(newVal)) {
					args.response.append("2FA DB field update failed\n");
				} else {
					args.response.append("2FA successfully enabled.\nYou will need to reconnect.\n");
				}
				return;
			}
		} else if (str.equalsIgnoreCase("confirm_disable_2fa")) {
			if (args.remaining()) {
				args.response.append("No parameters to confirm_disable_2fa\n");
				return;
			}
			if (!args.sender.has2FAAuthed()) {
				args.response.append("You must have authenticated with 2FA to disable it.\n");
				return;
			}
			try (INatsueUserData.LongTermPrivileged userData = args.hub.openUserDataByUINLT(args.senderUIN)) {
				if (userData == null) {
					args.response.append("How do you not exist?\n");
					return;
				}
				if (!userData.update2FA(0)) {
					args.response.append("2FA DB field update failed\n");
				} else {
					args.response.append("2FA successfully disabled.\n");
				}
				return;
			}
		}
		if (args.remaining()) {
			args.response.append("Don't want anything else\n");
			return;
		}
		int code;
		try {
			code = Integer.parseInt(str);
		} catch (Exception ex) {
			args.response.append("Code is not a valid number\n");
			return;
		}
		if (args.sender.try2FAAuth(code)) {
			args.response.append("Success!\n");
		} else if (!args.sender.has2FAConfigured()) {
			args.response.append("2FA not configured.\n");
		} else {
			args.response.append("Code invalid.\n");
		}
	}
}
