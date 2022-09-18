/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import natsue.config.IConfigProvider;
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;
import natsue.log.ILogProvider;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.INatsueDatabase.UserInfo;
import natsue.server.session.ISessionClient;

/**
 * Class that contains everything important to everything ever.
 */
public class ServerHub implements IHub {
	public static final long SERVER_UIN = UINUtils.make(UINUtils.HID_SYSTEM, 1);

	public final IConfigProvider config;
	public final ILogProvider log;
	public final INatsueDatabase database;

	public ServerHub(IConfigProvider cfg, ILogProvider logProvider, INatsueDatabase db) {
		config = cfg;
		log = logProvider;
		database = db;
	}

	@Override
	public String getNameByUIN(long uin) {
		if (uin == SERVER_UIN)
			return "Server";
		if (UINUtils.hid(uin) == UINUtils.HID_USER) {
			UserInfo ui = database.getUserByUID(UINUtils.uid(uin));
			if (ui != null)
				return ui.username;
		}
		return null;
	}

	@Override
	public long usernameAndPasswordToUIN(String username, String password) {
		UserInfo ui = database.getUserByUsername(username);
		if (ui == null)
			return 0;
		// this isn't how this is supposed to work but let's ignore that right now
		if (ui.passwordHash != null)
			if (ui.passwordHash.equals(password))
				return UINUtils.make(ui.uid, UINUtils.HID_USER);
		return 0;
	}

	@Override
	public long getServerUIN() {
		return SERVER_UIN;
	}

	@Override
	public boolean login(IHubClient cc) {
		return true;
	}

	@Override
	public void logout(IHubClient cc) {
	}

}
