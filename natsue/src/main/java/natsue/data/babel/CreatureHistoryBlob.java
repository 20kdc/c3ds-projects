/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.babel;

import java.nio.ByteBuffer;

import natsue.names.CreatureDataVerifier;

/**
 * Data on a creature's history!
 */
public class CreatureHistoryBlob {
	public final String moniker;
	public final int[] state;
	public final LifeEvent[] events;
	public final String name;
	public final String userText;

	public CreatureHistoryBlob(ByteBuffer dataToRead, int maxEventCount) {
		moniker = PacketReader.getString(dataToRead);
		if (dataToRead.get() != 0) {
			state = new int[5];
			for (int i = 0; i < 5; i++)
				state[i] = dataToRead.getInt();
		} else {
			state = null;
		}
		int eventCount = dataToRead.getInt();
		// 11 ints (specifically including the string lengths)
		int maximumEventCount = dataToRead.remaining() / 44;
		if (eventCount > maximumEventCount)
			throw new RuntimeException("Event count can't possibly be met by the data, assuming something screwy");
		if (eventCount > maxEventCount)
			throw new RuntimeException("Event count is above configured limit");
		events = new LifeEvent[eventCount];
		for (int i = 0; i < events.length; i++) {
			int eventType = dataToRead.getInt();
			int worldTime = dataToRead.getInt();
			int ageTicks = dataToRead.getInt();
			int unixTime = dataToRead.getInt();
			int unknown = dataToRead.getInt();
			String evMon1 = PacketReader.getString(dataToRead);
			String evMon2 = PacketReader.getString(dataToRead);
			String evWorldName = PacketReader.getString(dataToRead);
			String evWorldID = PacketReader.getString(dataToRead);
			String evUserID = PacketReader.getString(dataToRead);
			int index = dataToRead.getInt();
			events[i] = new LifeEvent(eventType, worldTime, ageTicks, unixTime, unknown, evMon1, evMon2, evWorldName, evWorldID, evUserID, index);
		}
		name = PacketReader.getString(dataToRead);
		userText = PacketReader.getString(dataToRead);
	}

	/**
	 * Just some sanity checks.
	 * Returns non-null on error
	 */
	public String verifySanity() {
		if (!CreatureDataVerifier.verifyMoniker(moniker))
			return "Moniker"; 
		return null;
	}

	public static class LifeEvent {
		public final int eventType, worldTime, ageTicks, unixTime, unknown;
		public final String mon1, mon2, worldName, worldID, userID;
		public final int index;
		public LifeEvent(int a, int b, int c, int d, int e, String f, String g, String h, String i, String j, int k) {
			eventType = a;
			worldTime = b;
			ageTicks = c;
			unixTime = d;
			unknown = e;
			mon1 = f;
			mon2 = g;
			worldName = h;
			worldID = i;
			userID = j;
			index = k;
		}
	}
}
