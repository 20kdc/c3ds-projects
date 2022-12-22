/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.system.cmd;

/**
 * Checks for oddities
 */
public class SystemCheckBotCommand extends BaseBotCommand {
	public SystemCheckBotCommand() {
		super("systemcheck", "",
				"Debugging",
				"Performs a system check on the Natsue server internals.",
				"",
				Cat.Admin);
	}

	@Override
	public void run(Context args) {
		args.response.append(args.hub.runSystemCheck(false));
	}
}
