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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

import natsue.data.babel.CreatureHistoryBlob;
import natsue.data.babel.UINUtils;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.INatsueUserFlags;
import natsue.server.database.NatsueDBCreatureEvent;
import natsue.server.database.NatsueDBCreatureInfo;
import natsue.server.database.NatsueDBWorldInfo;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.system.SystemCommands;
import natsue.server.userdata.INatsueUserData;

/**
 * The standard HTTP services
 */
public class HTTPHandlerImpl implements IHTTPHandler {
	public final IHubPrivilegedAPI hub;
	public final INatsueDatabase database;
	public HTTPHandlerImpl(IHubPrivilegedAPI sh, INatsueDatabase actualDB) {
		hub = sh;
		database = actualDB;
	}

	@Override
	public void handleHTTPGet(String url, boolean head, Client r) throws IOException {
		int qsIndex = url.indexOf('?');
		String[] qs = new String[] {""};
		if (qsIndex != -1) {
			qs = url.substring(qsIndex + 1).split("\\&");
			url = url.substring(0, qsIndex);
		}
		if (url.equals("/")) {
			StringBuilder rsp = new StringBuilder();
			rsp.append("<ul>\n");
			for (INatsueUserData nud : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
				rsp.append("<li>");
				userReference(rsp, nud);
				rsp.append("</li>\n");
			}
			rsp.append("</ul>\n");
			kisspopUI(r, head, "Current Online Users", rsp.toString());
		} else if (url.equals("/creature")) {
			if (qs.length < 1) {
				failInvalidRequestFormat(r, head);
				return;
			}
			HashMap<String, String> creatureNameCache = new HashMap<>();
			StringBuilder rsp = new StringBuilder();
			String fragment = HTMLEncoder.urlDecode(qs[0]);
			StringBuilder ttl = new StringBuilder();
			ttl.append("Creature ");
			HTMLEncoder.htmlEncode(ttl, fragment);
			NatsueDBCreatureInfo ci = database.getCreatureInfo(fragment);
			if (ci != null) {
				rsp.append("General Information:<ul>\n");
				rsp.append("<li>Name: ");
				HTMLEncoder.htmlEncode(rsp, ci.name);
				rsp.append("</li>");
				rsp.append("<li>User Text: ");
				HTMLEncoder.htmlEncode(rsp, ci.userText);
				rsp.append("</li>");
				rsp.append("<li>First Seen By: ");
				userReference(rsp, UINUtils.make(ci.senderUID, UINUtils.HID_USER));
				rsp.append("</li>");
				rsp.append("<li>Sex: ");
				switch (ci.state[CreatureHistoryBlob.STATE_SEX]) {
				case CreatureHistoryBlob.SEX_MALE:
					rsp.append("Male");
					break;
				case CreatureHistoryBlob.SEX_FEMALE:
					rsp.append("Female");
					break;
				default:
					rsp.append(ci.state[CreatureHistoryBlob.STATE_SEX]);
					break;
				}
				rsp.append("</li>");
				rsp.append("<li>Genus: ");
				switch (ci.state[CreatureHistoryBlob.STATE_GENUS]) {
				case CreatureHistoryBlob.GENUS_NORN:
					rsp.append("Norn");
					break;
				case CreatureHistoryBlob.GENUS_GRENDEL:
					rsp.append("Grendel");
					break;
				case CreatureHistoryBlob.GENUS_ETTIN:
					rsp.append("Ettin");
					break;
				case CreatureHistoryBlob.GENUS_GEAT:
					rsp.append("Geat");
					break;
				default:
					rsp.append(ci.state[CreatureHistoryBlob.STATE_GENUS]);
					break;
				}
				rsp.append("</li>");
				rsp.append("<li>Variant: ");
				rsp.append(ci.state[CreatureHistoryBlob.STATE_VARIANT]);
				rsp.append("</li>");
				rsp.append("<li>Point Mutations: ");
				rsp.append(ci.state[CreatureHistoryBlob.STATE_POINT_MUTATIONS]);
				rsp.append("</li>");
				rsp.append("<li>Crossover Points: ");
				rsp.append(ci.state[CreatureHistoryBlob.STATE_CROSSOVER_POINTS]);
				rsp.append("</li>");
				rsp.append("</ul>\n");
			} else {
				rsp.append("General Information not available<br/>\n");
			}
			LinkedList<NatsueDBCreatureEvent> ll = database.getCreatureEvents(fragment);
			if (ll != null) {
				rsp.append("Events:<br/><br/><table border=1>\n");
				rsp.append("<tr><td>Index</td><td>World Time</td><td>Age</td>\n");
				rsp.append("<td>Date/Time (dd/mm/yyyy)</td><td>Life Stage</td><td>Detail</td><td>World</td>\n");
				rsp.append("<td>Owner</td></tr>\n");
				for (NatsueDBCreatureEvent ev : ll) {
					rsp.append("<tr>");
					long senderUIN = UINUtils.make(ev.senderUID, UINUtils.HID_USER);
					//
					rsp.append("<td>");
					rsp.append(ev.eventIndex);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					writeTicks(rsp, ev.worldTime);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					writeTicks(rsp, ev.ageTicks);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z", Locale.ENGLISH);
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					rsp.append(df.format(new Date(ev.unixTime * 1000L)));
					rsp.append("</td>");
					//
					rsp.append("<td>");
					writeLifeStage(rsp, ev.lifeStage);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					writeEventDetails(rsp, ev, creatureNameCache);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					writeWorldReference(rsp, ev.worldID, ev.worldName);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					long atUIN = UINUtils.valueOf(ev.userID);
					userReference(rsp, atUIN);
					if (senderUIN != atUIN) {
						rsp.append("<br/>Sent By:<br/>");
						userReference(rsp, senderUIN);
					}
					rsp.append("</td>");
					//
					rsp.append("</tr>\n");
				}
				rsp.append("</table>\n");
			}
			kisspopUI(r, head, ttl.toString(), rsp.toString());
		} else if (url.equals("/world")) {
			if (qs.length < 1) {
				failInvalidRequestFormat(r, head);
				return;
			}
			HashMap<String, String> creatureNameCache = new HashMap<>();

			int limit = 50;
			int offset = 0;
			String worldID = HTMLEncoder.urlDecode(qs[0]);
			if (qs.length >= 2)
				offset = Integer.parseInt(qs[1]);

			StringBuilder header = new StringBuilder();
			StringBuilder rsp = new StringBuilder();

			String worldName = null;

			rsp.append("Known creatures (showing " + limit + " at " + offset + "):<ul>\n");
			LinkedList<String> m = database.getCreaturesInWorld(worldID, limit, offset);
			boolean shouldHaveNext = false;
			if (m != null) {
				shouldHaveNext = m.size() == limit;
				for (String moniker : m) {
					rsp.append("<li>");
					writeCreatureReference(rsp, moniker, creatureNameCache);
					if (worldName == null) {
						LinkedList<NatsueDBCreatureEvent> evl = database.getCreatureEvents(moniker);
						if (evl != null) {
							for (NatsueDBCreatureEvent ev : evl) {
								if (ev.worldID.equals(worldID)) {
									worldName = ev.worldName;
									header.append("Owner: ");
									userReference(header, UINUtils.valueOf(ev.userID));
									header.append("<br/>");
									break;
								}
							}
						}
					}
					rsp.append("</li>\n");
				}
			}
			rsp.append("</ul>\n");

			writePager(rsp, offset, limit, shouldHaveNext, "world?" + HTMLEncoder.hrefEncode(worldID));

			StringBuilder ttl = new StringBuilder();
			ttl.append("World ");
			if (worldName != null) {
				HTMLEncoder.htmlEncode(ttl, worldName);
				ttl.append(" (");
				HTMLEncoder.htmlEncode(ttl, worldID);
				ttl.append(")");
			} else {
				HTMLEncoder.htmlEncode(ttl, worldID);
			}

			kisspopUI(r, head, ttl.toString(), header + rsp.toString());
		} else if (url.equals("/user")) {
			if (qs.length < 1) {
				failInvalidRequestFormat(r, head);
				return;
			}
			int limit = 50;
			int offset = 0;
			String userRef = HTMLEncoder.urlDecode(qs[0]);
			if (qs.length >= 2)
				offset = Integer.parseInt(qs[1]);

			StringBuilder rsp = new StringBuilder();
			String fragment = HTMLEncoder.urlDecode(userRef);
			INatsueUserData data = hub.getUserDataByNickname(fragment);
			if (data == null)
				data = hub.getUserDataByUIN(UINUtils.valueOf(fragment));
			String title;
			if (data == null) {
				title = "No such user";
			} else {
				StringBuilder ttl = new StringBuilder();
				ttl.append("User ");
				HTMLEncoder.htmlEncode(ttl, data.getNickname());
				ttl.append(" (");
				HTMLEncoder.htmlEncode(ttl, data.getUINString());
				ttl.append(")");
				title = ttl.toString();
				if (hub.isUINOnline(data.getUIN())) {
					rsp.append("Online<br/>");
				} else {
					rsp.append("Offline<br/>");
				}
				rsp.append("Flags: " + INatsueUserFlags.Flag.showFlags(data.getFlags()) + "<br/>");

				if (UINUtils.isRegularUser(data.getUIN())) {
					rsp.append("Known worlds (showing " + limit + " at " + offset + "):<ul>\n");
					LinkedList<NatsueDBWorldInfo> m = database.getWorldsInUser(UINUtils.uid(data.getUIN()), limit, offset);
					boolean shouldHaveNext = false;
					if (m != null) {
						shouldHaveNext = m.size() == limit;
						for (NatsueDBWorldInfo world : m) {
							rsp.append("<li>");
							writeWorldReference(rsp, world.worldID, world.worldName);
							rsp.append("</li>\n");
						}
					}
					rsp.append("</ul>\n");
	
					writePager(rsp, offset, limit, shouldHaveNext, "user?" + HTMLEncoder.hrefEncode(userRef));
				}
			}
			kisspopUI(r, head, title, rsp.toString());
		} else {
			kisspopUI(r, head, "404 Not Found", "No such file...", "");
		}
	}

	private void writeWorldReference(StringBuilder rsp, String worldID, String worldName) {
		rsp.append("<a href=\"world?" + HTMLEncoder.hrefEncode(worldID) + "\">");
		HTMLEncoder.htmlEncode(rsp, worldName);
		rsp.append("</a>");
	}

	private void writePager(StringBuilder rsp, int offset, int limit, boolean shouldHaveNext, String lBase) {
		int prevOfs = offset - limit;
		if (prevOfs >= 0) {
			rsp.append("<a href=\"");
			rsp.append(lBase);
			rsp.append("&");
			rsp.append(prevOfs);
			rsp.append("\">prev</a>");
			if (shouldHaveNext)
				rsp.append(" ");
		}
		if (shouldHaveNext) {
			rsp.append("<a href=\"");
			rsp.append(lBase);
			rsp.append("&");
			rsp.append(offset + limit);
			rsp.append("\">next</a>");
		}
		rsp.append("\n");
	}

	private void failInvalidRequestFormat(Client r, boolean head) throws IOException {
		kisspopUI(r, head, "Invalid Request Format", "Some component of the request was wrong.");
	}

	private void kisspopUI(Client r, boolean head, String title, String text) throws IOException {
		kisspopUI(r, head, "200 OK", title, text);
	}

	private void kisspopUI(Client r, boolean head, String status, String title, String text) throws IOException {
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

	private void writeEventDetails(StringBuilder rsp, NatsueDBCreatureEvent ev, HashMap<String, String> creatureNameCache) {
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

	private void writeCreatureReference(StringBuilder rsp, String src, HashMap<String, String> creatureNameCache) {
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

	private void writeLifeStage(StringBuilder rsp, int lifeStage) {
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

	private void writeTicks(StringBuilder rsp, int ageTicks) {
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

	private void userReference(StringBuilder rsp, long uin) {
		INatsueUserData nud = hub.getUserDataByUIN(uin);
		if (nud == null) {
			String ts = UINUtils.toString(uin);
			userReference(rsp, uin, ts, ts + "?");
		} else {
			userReference(rsp, nud);
		}
	}
	private void userReference(StringBuilder rsp, INatsueUserData nud) {
		userReference(rsp, nud.getUIN(), nud.getNicknameFolded(), nud.getNickname());
	}
	private void userReference(StringBuilder rsp, long uin, String nf, String nn) {
		rsp.append("<a href=\"user?" + HTMLEncoder.hrefEncode(nf) + "\">");
		HTMLEncoder.htmlEncode(rsp, nn);
		rsp.append("</a>");
	}
}
