/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.database;

/**
 * So that this can be retrieved easily.
 */
public class NatsueDBCreatureEvent {
	public final int senderUID;
	public final String moniker;
	public final int eventIndex;
	public final int eventType;
	public final int worldTime;
	public final int ageTicks;
	public final int unixTime;
	public final int lifeStage;
	public final String param1, param2, worldName, worldID, userID;

	/**
	 * Main constructor. When changing this, be aware of it's callers!
	 */
	public NatsueDBCreatureEvent(int senderUID, String moniker, int eventIndex, int eventType, int worldTime, int ageTicks, int unixTime, int lifeStage, String param1, String param2, String worldName, String worldID, String userID) {
		this.senderUID = senderUID;
		this.moniker = moniker;
		this.eventIndex = eventIndex;
		this.eventType = eventType;
		this.worldTime = worldTime;
		this.ageTicks = ageTicks;
		this.unixTime = unixTime;
		this.lifeStage = lifeStage;
		this.param1 = param1;
		this.param2 = param2;
		this.worldName = worldName;
		this.worldID = worldID;
		this.userID = userID;
	}
}
