/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

import natsue.data.babel.CreatureHistoryBlob.LifeEvent;

/**
 * Information on a creature.
 */
public class NatsueDBCreatureInfo {
	public final String moniker;
	public final int senderUID;
	public final int ch0, ch1, ch2, ch3, ch4;
	public final String name;
	public final String userText;

	public NatsueDBCreatureInfo(String m, int u, int c0, int c1, int c2, int c3, int c4, String n, String ut) {
		moniker = m;
		senderUID = u;
		ch0 = c0;
		ch1 = c1;
		ch2 = c2;
		ch3 = c3;
		ch4 = c4;
		name = n;
		userText = ut;
	}
}
