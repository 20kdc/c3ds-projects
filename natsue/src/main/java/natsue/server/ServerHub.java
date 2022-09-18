/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server;

import natsue.IConfigProvider;
import natsue.ILogProvider;
import natsue.data.babel.ctos.PacketReader;
import natsue.server.database.INatsueDatabase;

/**
 * Class that contains everything important to everything ever.
 */
public class ServerHub {
	public final IConfigProvider config;
	public final ILogProvider log;
	public final INatsueDatabase database;
	public final PacketReader packetReader;

	public ServerHub(IConfigProvider cfg, ILogProvider logProvider, INatsueDatabase db) {
		config = cfg;
		log = logProvider;
		database = db;
		packetReader = new PacketReader(config);
	}
}
