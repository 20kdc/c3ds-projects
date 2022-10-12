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
	// Immutable state block indices
	public static final int STATE_SEX = 0;
	public static final int STATE_GENUS = 1;
	public static final int STATE_VARIANT = 2;
	public static final int STATE_POINT_MUTATIONS = 3;
	public static final int STATE_CROSSOVER_POINTS = 4;
	// values
	public static final int SEX_MALE = 1;
	public static final int SEX_FEMALE = 2;
	public static final int GENUS_NORN = 0;
	public static final int GENUS_GRENDEL = 1;
	public static final int GENUS_ETTIN = 2;
	public static final int GENUS_GEAT = 3;
	// for convenience
	public static final int EV_O_CONCEIVED = 0;
	public static final int EV_O_SPLICED = 1;
	public static final int EV_O_SYNTHESIZED = 2;
	public static final int EV_BORN = 3;
	public static final int EV_AGED = 4;
	public static final int EV_IMPORTED = 5;
	public static final int EV_EXPORTED = 6;
	public static final int EV_DIED = 7;
	public static final int EV_PREGNANT_SELF = 8;
	public static final int EV_PREGNANT_OTHER = 9;
	public static final int EV_O_CLONED = 14;
	public static final int EV_CLONED_TO = 15;
	public static final int EV_WARP_OUT = 16;
	public static final int EV_WARP_IN = 17;

	/**
	 * Creature's moniker.
	 */
	public final String moniker;

	/**
	 * Immutable state values. May be null (as this is only supposed to be sent once!)
	 */
	public final int[] state;

	/**
	 * List of events.
	 */
	public final LifeEvent[] events;

	/**
	 * Name - if empty, might not be valid!
	 */
	public final String name;

	/**
	 * User text, can be null
	 */
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
		if (dataToRead.getInt() != 0) {
			userText = PacketReader.getString(dataToRead);
		} else {
			userText = null;
		}
	}

	/**
	 * Just some sanity checks.
	 * Returns non-null on error
	 */
	public String verifySanity() {
		if (!CreatureDataVerifier.verifyMoniker(moniker))
			return "Moniker"; 
		for (LifeEvent le : events)
			if (le.index < 0)
				return "Event index under 0";
		return null;
	}

	public static class LifeEvent {
		public final int eventType, worldTime, ageTicks, unixTime, lifeStage;
		public final String mon1, mon2, worldName, worldID, userID;
		public final int index;
		public LifeEvent(int a, int b, int c, int d, int e, String f, String g, String h, String i, String j, int k) {
			eventType = a;
			worldTime = b;
			ageTicks = c;
			unixTime = d;
			lifeStage = e;
			mon1 = f;
			mon2 = g;
			worldName = h;
			worldID = i;
			userID = j;
			index = k;
		}
	}
}
