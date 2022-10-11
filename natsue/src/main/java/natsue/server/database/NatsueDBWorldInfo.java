/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

/**
 * Information on a world.
 * Note that this isn't a formal DB struct *yet* but it's written in the form one would take.
 */
public class NatsueDBWorldInfo {
	public final int ownerUID;
	public final String worldID;
	public final String worldName;

	public NatsueDBWorldInfo(int o, String n, String ut) {
		ownerUID = o;
		worldID = n;
		worldName = ut;
	}
}
