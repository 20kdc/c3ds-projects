/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.http;

import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.UINUtils;
import natsue.server.database.NatsueDBCreatureEvent;
import natsue.server.database.NatsueDBCreatureInfo;
import natsue.server.database.NatsueDBWorldInfo;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.userdata.INatsueUserData;

/**
 * For JSON encoding
 */
class Resources {

	public static void encodeUser(JSONEncoder je, INatsueUserData nud, IHubPrivilegedAPI hub) {
		je.objectStart();
		je.writeKV("uin", nud.getUINString());
		je.writeKV("exists", true);
		je.writeKV("online", hub.isUINOnline(nud.getUIN()));
		// note: below disappear if the user doesn't exist, i.e. encodeUserByUIN
		je.writeKV("nickname", nud.getNickname());
		je.writeKV("nicknameFolded", nud.getNicknameFolded());
		je.writeKV("flags", nud.getFlags());
		je.objectEnd();
	}

	public static INatsueUserData encodeUserByUIN(JSONEncoder je, long make, IHubPrivilegedAPI hub) {
		return encodeUserByUIN(je, make, hub, null);
	}
	public static INatsueUserData encodeUserByUIN(JSONEncoder je, long make, IHubPrivilegedAPI hub, INatsueUserData cache) {
		INatsueUserData nud = null;
		if (cache != null && cache.getUIN() == make)
			nud = cache;
		else
			nud = hub.getUserDataByUIN(make);
		if (nud != null) {
			encodeUser(je, nud, hub);
		} else {
			je.objectStart();
			je.writeKV("uin", UINUtils.toString(make));
			je.writeKV("exists", false);
			je.writeKV("online", false);
			je.objectEnd();
		}
		return nud;
	}

	public static INatsueUserData encodeUserByUID(JSONEncoder je, int uid, IHubPrivilegedAPI hub) {
		return encodeUserByUID(je, uid, hub, null);
	}
	public static INatsueUserData encodeUserByUID(JSONEncoder je, int uid, IHubPrivilegedAPI hub, INatsueUserData cache) {
		return encodeUserByUIN(je, UINUtils.ofRegularUser(uid), hub, cache);
	}

	public static void encodeCreatureInfoFail(JSONEncoder je, String moniker) {
		je.objectStart();
		je.writeKV("moniker", moniker);
		je.objectEnd();
	}
	public static void encodeCreatureInfo(JSONEncoder je, NatsueDBCreatureInfo ci, IHubPrivilegedAPI hub) {
		je.objectStart();
		// "sender": User?
		je.write("sender");
		je.objectSplit();
		encodeUserByUID(je, ci.senderUID, hub);
		// continue
		je.writeKV("moniker", ci.moniker); // ONLY THIS SURVIVES ON FAILURE
		je.writeKV("name", ci.name);
		je.writeKV("userText", ci.userText);
		// "state": CreatureState?
		je.write("state");
		je.objectSplit();
		je.objectStart();
		je.writeKV("sex", ci.state[CreatureHistoryBlob.STATE_SEX]);
		je.writeKV("genus", ci.state[CreatureHistoryBlob.STATE_GENUS]);
		je.writeKV("variant", ci.state[CreatureHistoryBlob.STATE_VARIANT]);
		je.writeKV("pointMutations", ci.state[CreatureHistoryBlob.STATE_POINT_MUTATIONS]);
		je.writeKV("crossoverPoints", ci.state[CreatureHistoryBlob.STATE_CROSSOVER_POINTS]);
		je.objectEnd();
		// done!
		je.objectEnd();
	}

	public static void encodeCreatureEvent(JSONEncoder je, NatsueDBCreatureEvent ev, IHubPrivilegedAPI hub) {
		je.objectStart();
		// "sender": User
		je.write("sender");
		je.objectSplit();
		INatsueUserData cache = encodeUserByUID(je, ev.senderUID, hub);
		// continue
		je.writeKV("moniker", ev.moniker);
		je.writeKV("eventIndex", ev.eventIndex);
		je.writeKV("eventType", ev.eventType);
		je.writeKV("worldTime", ev.worldTime);
		je.writeKV("ageTicks", ev.ageTicks);
		je.writeKV("unixTime", ev.unixTime);
		je.writeKV("lifeStage", ev.lifeStage);
		je.writeKV("param1", ev.param1);
		je.writeKV("param2", ev.param2);
		// "world": World
		je.write("world");
		je.objectSplit();
		encodeWorldByDetails(je, ev.worldName, ev.worldID, UINUtils.valueOf(ev.userID), hub, cache);
		// done!
		je.objectEnd();
	}

	public static void encodeWorldByDetails(JSONEncoder je, String worldName, String worldID, long uin, IHubPrivilegedAPI hub, INatsueUserData cache) {
		je.objectStart();
		je.writeKV("id", worldID);
		je.writeKV("name", worldName);
		// "user": User|null
		je.write("user");
		je.objectSplit();
		if (uin != -1) {
			encodeUserByUIN(je, uin, hub, cache);
		} else {
			je.write(null);
		}
		// Done!
		je.objectEnd();
	}
	public static INatsueUserData encodeWorld(JSONEncoder je, NatsueDBWorldInfo wi, IHubPrivilegedAPI pa, INatsueUserData cache) {
		je.objectStart();
		je.writeKV("id", wi.worldID);
		je.writeKV("name", wi.worldName);
		// "user": User|null
		je.write("user");
		je.objectSplit();
		INatsueUserData ch = encodeUserByUIN(je, UINUtils.ofRegularUser(wi.ownerUID), pa, cache);
		// Done!
		je.objectEnd();
		return ch;
	}
}
