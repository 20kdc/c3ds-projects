/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.UINUtils;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.NatsueDBCreatureEvent;
import natsue.server.database.NatsueDBCreatureInfo;
import natsue.server.http.IHTTPHandler.Client;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.system.SystemCommands;
import natsue.server.userdata.INatsueUserData;

/**
 * Stuff for the HTTP server pages
 */
class Pages {
	final IHubPrivilegedAPI hub;
	final INatsueDatabase database;

	Pages(IHubPrivilegedAPI h, INatsueDatabase d) {
		hub = h;
		database = d;
	}

	void writeWorldReference(StringBuilder rsp, String worldID, String worldName) {
		rsp.append("<a href=\"world?" + HTMLEncoder.hrefEncode(worldID) + "\">");
		HTMLEncoder.htmlEncode(rsp, worldName);
		rsp.append("</a>");
	}

	void writePager(StringBuilder rsp, int offset, int limit, boolean shouldHaveNext, String lBase) {
		int prevOfs = offset - limit;
		if (prevOfs >= 0) {
			rsp.append("<a href=\"");
			rsp.append(lBase);
			rsp.append("&offset=");
			rsp.append(prevOfs);
			rsp.append("\">prev</a>");
			if (shouldHaveNext)
				rsp.append(" ");
		}
		if (shouldHaveNext) {
			rsp.append("<a href=\"");
			rsp.append(lBase);
			rsp.append("&offset=");
			rsp.append(offset + limit);
			rsp.append("\">next</a>");
		}
		rsp.append("\n");
	}

	void failInvalidRequestFormat(Client r, boolean head) throws IOException {
		kisspopUI(r, head, "Invalid Request Format", "Some component of the request was wrong.");
	}

	void kisspopUI(Client r, boolean head, String title, String text) throws IOException {
		kisspopUI(r, head, "200 OK", title, text);
	}

	void kisspopUI(Client r, boolean head, String status, String title, String text) throws IOException {
		StringBuilder finale = new StringBuilder();
		try (FileInputStream fis = new FileInputStream("kisspopui.html")) {
			InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
			while (true) {
				int ch = isr.read();
				if (ch == -1)
					break;
				finale.append((char) ch);
			}
		} catch (Exception ex) {
			// :(
		}
		String sts = finale.toString();

		// What do you mean it wasn't supposed to be literal?
		if (!sts.contains("$SHIT_GOES_HERE"))
			sts += "$PAGE_TITLE<hr/>$SHIT_GOES_HERE<hr/><i>Server Version: $NATSUE_VERSION</i><hr/>kisspopui.html invalid or missing $SHIT_GOES_HERE";
		sts = sts.replaceFirst("\\$NATSUE_VERSION", "<a href=\"" + SystemCommands.VERSION_URL + "\">" + SystemCommands.VERSION + "</a>");
		sts = sts.replaceAll("\\$PAGE_TITLE", title);
		// MUST BE LAST!
		sts = sts.replaceFirst("\\$SHIT_GOES_HERE", text);
		r.httpResponse(status, head, "text/html", sts);
	}

	void writeEventDetails(StringBuilder rsp, NatsueDBCreatureEvent ev, HashMap<String, String> creatureNameCache) {
		if (ev.eventType == CreatureHistoryBlob.EV_O_CONCEIVED) {
			rsp.append("Conceived: ");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
			rsp.append(" x ");
			writeCreatureReference(rsp, ev.param2, creatureNameCache);
		} else if (ev.eventType == CreatureHistoryBlob.EV_O_SPLICED) {
			rsp.append("Spliced: ");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
			rsp.append(" x ");
			writeCreatureReference(rsp, ev.param2, creatureNameCache);
		} else if (ev.eventType == CreatureHistoryBlob.EV_O_SYNTHESIZED) {
			rsp.append("Synthesized: ");
			HTMLEncoder.htmlEncode(rsp, ev.param2);
		} else if (ev.eventType == CreatureHistoryBlob.EV_BORN) {
			rsp.append("Born");
		} else if (ev.eventType == CreatureHistoryBlob.EV_AGED) {
			rsp.append("Aged");
		} else if (ev.eventType == CreatureHistoryBlob.EV_EXPORTED) {
			rsp.append("Exported");
		} else if (ev.eventType == CreatureHistoryBlob.EV_IMPORTED) {
			rsp.append("Imported");
		} else if (ev.eventType == CreatureHistoryBlob.EV_DIED) {
			rsp.append("Died");
		} else if (ev.eventType == CreatureHistoryBlob.EV_PREGNANT_SELF) {
			rsp.append("Pregnant w/ ");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
			rsp.append(" due to ");
			writeCreatureReference(rsp, ev.param2, creatureNameCache);
		} else if (ev.eventType == CreatureHistoryBlob.EV_PREGNANT_OTHER) {
			rsp.append("Impregnated ");
			writeCreatureReference(rsp, ev.param2, creatureNameCache);
			rsp.append(" with ");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
		} else if (ev.eventType == CreatureHistoryBlob.EV_O_CLONED) {
			rsp.append("Clone of: ");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
		} else if (ev.eventType == CreatureHistoryBlob.EV_CLONED_TO) {
			rsp.append("Cloned to ");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
		} else if (ev.eventType == CreatureHistoryBlob.EV_WARP_OUT) {
			rsp.append("Warped out");
		} else if (ev.eventType == CreatureHistoryBlob.EV_WARP_IN) {
			rsp.append("Warped in");
		} else {
			rsp.append("<table><tr><td>");
			rsp.append(ev.eventType);
			rsp.append("</td><td>");
			writeCreatureReference(rsp, ev.param1, creatureNameCache);
			rsp.append("</td><td>");
			writeCreatureReference(rsp, ev.param2, creatureNameCache);
			rsp.append("</td></tr></table>");
		}
	}

	void writeCreatureReference(StringBuilder rsp, String src, HashMap<String, String> creatureNameCache) {
		String name = creatureNameCache.get(src);
		if (name == null) {
			NatsueDBCreatureInfo ci = database.getCreatureInfo(src);
			if ((ci != null) && (!ci.name.equals(""))) {
				int hasPfx = src.indexOf('-');
				if (hasPfx == -1) {
					name = "?-" + ci.name;
				} else {
					name = src.substring(0, hasPfx + 1) + ci.name;
				}
			} else {
				name = src;
			}
			creatureNameCache.put(src, name);
		}
		rsp.append("<a href=\"creature?" + HTMLEncoder.hrefEncode(src) + "\">");
		HTMLEncoder.htmlEncode(rsp, name);
		rsp.append("</a>");
	}

	void writeLifeStage(StringBuilder rsp, int lifeStage) {
		if (lifeStage == -1) {
			rsp.append("Egg");
		} else if (lifeStage == 0) {
			rsp.append("Baby");
		} else if (lifeStage == 1) {
			rsp.append("Child");
		} else if (lifeStage == 2) {
			rsp.append("Adolescent");
		} else if (lifeStage == 3) {
			rsp.append("Youth");
		} else if (lifeStage == 4) {
			rsp.append("Adult");
		} else if (lifeStage == 5) {
			rsp.append("Old");
		} else if (lifeStage == 6) {
			rsp.append("Ancient");
		} else if (lifeStage == 7) {
			rsp.append("Dead");
		} else {
			rsp.append(lifeStage);
		}
	}

	void writeTicks(StringBuilder rsp, int ageTicks) {
		int sec = ageTicks / 20;
		int min = sec / 60;
		sec %= 60;
		int hrs = min / 60;
		min %= 60;
		if (hrs > 0) {
			rsp.append(hrs);
			rsp.append("h");
		}
		if (min > 0) {
			rsp.append(min);
			rsp.append("m");
		}
		rsp.append(sec);
		rsp.append("s");
	}

	void userReference(StringBuilder rsp, long uin) {
		INatsueUserData nud = hub.getUserDataByUIN(uin);
		if (nud == null) {
			String ts = UINUtils.toString(uin);
			userReference(rsp, uin, ts, ts + "?");
		} else {
			userReference(rsp, nud);
		}
	}
	void userReference(StringBuilder rsp, INatsueUserData nud) {
		userReference(rsp, nud.getUIN(), nud.getNicknameFolded(), nud.getNickname());
	}
	void userReference(StringBuilder rsp, long uin, String nf, String nn) {
		rsp.append("<a href=\"user?" + HTMLEncoder.hrefEncode(nf) + "\">");
		HTMLEncoder.htmlEncode(rsp, nn);
		rsp.append("</a>");
	}

}
