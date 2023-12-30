/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.http;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import natsue.server.photo.IPhotoStorage;
import natsue.server.system.SystemCommands;
import natsue.server.userdata.INatsueUserData;

/**
 * The standard HTTP services
 */
public class HTTPHandlerImpl implements IHTTPHandler {
	public final int PAGE_SIZE_CREATURES = 50;
	public final int PAGE_SIZE_WORLDS = 50;
	public final int PAGE_SIZE_API_WORLD_CREATURES = 50;
	public final int PAGE_SIZE_API_USER_WORLDS = 50;

	public final IHubPrivilegedAPI hub;
	public final INatsueDatabase database;
	public final IPhotoStorage photoStorage;
	public final Pages pages;
	public final boolean apiPublic;
	public final boolean photosPublic;
	// This is converted from "" to null elsewhere.
	public final String apiKey;

	public HTTPHandlerImpl(IHubPrivilegedAPI sh, boolean ap, String apiKey, INatsueDatabase actualDB, IPhotoStorage photoStorage, boolean photosPublic) {
		hub = sh;
		database = actualDB;
		pages = new Pages(sh, actualDB);
		apiPublic = ap;
		this.apiKey = apiKey;
		this.photoStorage = photoStorage;
		this.photosPublic = photosPublic;
	}

	@Override
	public void handleHTTPGet(String url, boolean head, Client r) throws IOException {
		int qsIndex = url.indexOf('?');
		HashMap<String, String> qv = new HashMap<>();;
		if (qsIndex != -1) {
			HTMLEncoder.qsToVars(qv, url.substring(qsIndex + 1));
			url = url.substring(0, qsIndex);
		}
		boolean administrative = false;
		// Is an API key provided?
		if (apiKey != null) {
			String otherKey = qv.get("apiKey");
			if (otherKey != null)
				if (apiKey.equals(otherKey))
					administrative = true;
		}
		boolean apiAllowed = administrative || apiPublic || r.isLocal();
		if (pageSubhandler(url, qv, head, r)) {
			// handled
		} else if (phoSubhandler(url, qv, head, r, administrative)) {
		} else if (apiAllowed && apiSubhandler(url, qv, head, r)) {
			// handled
		} else {
			pages.kisspopUI(r, head, "404 Not Found", "No such file...", "");
		}
	}
	private boolean pageSubhandler(String url, HashMap<String, String> qv, boolean head, Client r) throws IOException {
		// This is probably not the best of designs.
		if (url.equals("/")) {
			StringBuilder rsp = new StringBuilder();
			rsp.append("<ul>\n");
			for (INatsueUserData nud : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
				rsp.append("<li>");
				pages.userReference(rsp, nud);
				rsp.append("</li>\n");
			}
			rsp.append("</ul>\n");
			pages.kisspopUI(r, head, "Current Online Users", rsp.toString());
		} else if (url.equals("/creature")) {
			String fragment = qv.get("p0");
			if (fragment == null) {
				pages.failInvalidRequestFormat(r, head);
				return true;
			}
			HashMap<String, String> creatureNameCache = new HashMap<>();
			StringBuilder rsp = new StringBuilder();
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
				pages.userReference(rsp, UINUtils.ofRegularUser(ci.senderUID));
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
					long senderUIN = UINUtils.ofRegularUser(ev.senderUID);
					//
					rsp.append("<td>");
					rsp.append(ev.eventIndex);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					pages.writeTicks(rsp, ev.worldTime);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					pages.writeTicks(rsp, ev.ageTicks);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z", Locale.ENGLISH);
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					rsp.append(df.format(new Date(ev.unixTime * 1000L)));
					rsp.append("</td>");
					//
					rsp.append("<td>");
					pages.writeLifeStage(rsp, ev.lifeStage);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					pages.writeEventDetails(rsp, ev, creatureNameCache);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					pages.writeWorldReference(rsp, ev.worldID, ev.worldName);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					long atUIN = UINUtils.valueOf(ev.userID);
					pages.userReference(rsp, atUIN);
					if (senderUIN != atUIN) {
						rsp.append("<br/>Sent By:<br/>");
						pages.userReference(rsp, senderUIN);
					}
					rsp.append("</td>");
					//
					rsp.append("</tr>\n");
				}
				rsp.append("</table>\n");
				if (photosPublic) {
					rsp.append("Photos:<br/><br/>\n");
					rsp.append("<table border=1>\n");
					for (Integer i : photoStorage.getIndices(fragment)) {
						rsp.append("<tr><td>\n");
						rsp.append("<img src=\"");
						rsp.append("creaturePhoto.png?moniker=");
						rsp.append(HTMLEncoder.hrefEncode(fragment));
						rsp.append("&index=");
						rsp.append(i);
						rsp.append("\"/>");
						rsp.append("</td></tr>\n");
					}
					rsp.append("</table>\n");
				}
			}
			pages.kisspopUI(r, head, ttl.toString(), rsp.toString());
		} else if (url.equals("/world")) {
			String worldID = qv.get("p0");
			if (worldID == null) {
				pages.failInvalidRequestFormat(r, head);
				return true;
			}
			HashMap<String, String> creatureNameCache = new HashMap<>();

			int limit = PAGE_SIZE_CREATURES;
			int offset = Integer.parseInt(qv.getOrDefault("offset", "0"));

			StringBuilder rsp = new StringBuilder();

			NatsueDBWorldInfo ci = database.getWorldInfo(worldID);
			if (ci != null) {
				rsp.append("Owner: ");
				pages.userReference(rsp, UINUtils.ofRegularUser(ci.ownerUID));
				rsp.append("<br/>");
			}

			rsp.append("Known creatures (showing " + limit + " at " + offset + "):<ul>\n");
			LinkedList<String> m = database.getCreaturesInWorld(worldID, limit, offset);
			boolean shouldHaveNext = false;
			if (m != null) {
				shouldHaveNext = m.size() == limit;
				for (String moniker : m) {
					rsp.append("<li>");
					pages.writeCreatureReference(rsp, moniker, creatureNameCache);
					rsp.append("</li>\n");
				}
			}
			rsp.append("</ul>\n");

			pages.writePager(rsp, offset, limit, shouldHaveNext, "world?" + HTMLEncoder.hrefEncode(worldID));

			StringBuilder ttl = new StringBuilder();
			ttl.append("World ");
			if (ci != null) {
				HTMLEncoder.htmlEncode(ttl, ci.worldName);
				ttl.append(" (");
				HTMLEncoder.htmlEncode(ttl, worldID);
				ttl.append(")");
			} else {
				HTMLEncoder.htmlEncode(ttl, worldID);
			}

			pages.kisspopUI(r, head, ttl.toString(), rsp.toString());
		} else if (url.equals("/user")) {
			String userRef = qv.get("p0");
			if (userRef == null) {
				pages.failInvalidRequestFormat(r, head);
				return true;
			}

			int limit = PAGE_SIZE_WORLDS;
			int offset = Integer.parseInt(qv.getOrDefault("offset", "0"));

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
							pages.writeWorldReference(rsp, world.worldID, world.worldName);
							rsp.append("</li>\n");
						}
					}
					rsp.append("</ul>\n");
	
					pages.writePager(rsp, offset, limit, shouldHaveNext, "user?" + HTMLEncoder.hrefEncode(userRef));
				}
			}
			pages.kisspopUI(r, head, title, rsp.toString());
		} else {
			return false;
		}
		return true;
	}

	private boolean phoSubhandler(String url, HashMap<String, String> qv, boolean head, Client r, boolean administrative) throws IOException {
		if (url.equals("/creaturePhoto.png")) {
			if (!(photosPublic || administrative))
				return false;
			String moniker = qv.get("moniker");
			if (moniker == null) {
				r.httpResponse("404 Not Found", head, "requires moniker=...");
				return true;
			}
			String index = qv.get("index");
			if (index == null) {
				r.httpResponse("404 Not Found", head, "requires index=...");
				return true;
			}
			byte[] data = photoStorage.getPhotoPNG(moniker, Integer.parseInt(index));
			if (data == null) {
				r.httpResponse("404 Not Found", head, "no such photo");
				return true;
			}
			r.httpResponse("200 OK", head, "image/png", data);
			return true;
		}
		return false;
	}

	private boolean apiSubhandler(String url, HashMap<String, String> qv, boolean head, Client r) throws IOException {
		if (url.equals("/api/index")) {
			JSONEncoder je = new JSONEncoder();
			je.objectStart();
			je.writeKV("version", SystemCommands.VERSION);
			je.writeKV("versionURL", SystemCommands.VERSION_URL);
			je.writeKV("pageSizeAPIWorldCreatures", PAGE_SIZE_API_WORLD_CREATURES);
			je.writeKV("pageSizeAPIUserWorlds", PAGE_SIZE_API_USER_WORLDS);
			je.objectEnd();
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/usersOnline")) {
			JSONEncoder je = new JSONEncoder();
			je.arrayStart();
			for (INatsueUserData nud : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem())
				Resources.encodeUser(je, nud, hub);
			je.arrayEnd();
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/creatureInfo")) {
			String fragment = qv.get("moniker");
			if (fragment == null) {
				r.httpResponse("404 Not Found", head, "requires moniker=...");
				return true;
			}
			NatsueDBCreatureInfo ci = database.getCreatureInfo(fragment);
			if (ci == null) {
				r.httpResponse("404 Not Found", head, "not in database");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			Resources.encodeCreatureInfo(je, ci, hub);
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/creatureEvents")) {
			String fragment = qv.get("moniker");
			if (fragment == null) {
				r.httpResponse("404 Not Found", head, "requires moniker=...");
				return true;
			}
			LinkedList<NatsueDBCreatureEvent> ci = database.getCreatureEvents(fragment);
			if (ci == null) {
				r.httpResponse("404 Not Found", head, "not in database");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			je.arrayStart();
			for (NatsueDBCreatureEvent ev : ci)
				Resources.encodeCreatureEvent(je, ev, hub);
			je.arrayEnd();
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/user")) {
			String uin = qv.get("uin");
			String nickname = qv.get("nickname");
			INatsueUserData data;
			if (uin != null) {
				data = hub.getUserDataByUIN(UINUtils.valueOf(uin));
			} else if (nickname != null) {
				data = hub.getUserDataByNickname(nickname);
			} else {
				r.httpResponse("404 Not Found", head, "requires uin=... or nickname=...");
				return true;
			}
			if (data == null) {
				r.httpResponse("404 Not Found", head, "unknown user");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			Resources.encodeUser(je, data, hub);
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/world")) {
			String id = qv.get("id");
			if (id == null) {
				r.httpResponse("404 Not Found", head, "requires moniker=...");
				return true;
			}
			NatsueDBWorldInfo ci = database.getWorldInfo(id);
			if (ci == null) {
				r.httpResponse("404 Not Found", head, "not in database");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			Resources.encodeWorld(je, ci, hub, null);
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/worldCreatures")) {
			String fragment = qv.get("id");
			String offset = qv.get("offset");
			if ((fragment == null) || (offset == null)) {
				r.httpResponse("404 Not Found", head, "requires id=... and offset=...");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			je.arrayStart();
			LinkedList<String> wir = database.getCreaturesInWorld(fragment, PAGE_SIZE_API_WORLD_CREATURES, Integer.parseInt(offset));
			if (wir != null) {
				for (String s : wir) {
					NatsueDBCreatureInfo ci = database.getCreatureInfo(s);
					if (ci != null) {
						Resources.encodeCreatureInfo(je, ci, hub);
					} else {
						Resources.encodeCreatureInfoFail(je, s);
					}
				}
			}
			je.arrayEnd();
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/userWorlds")) {
			String fragment = qv.get("uin");
			String offset = qv.get("offset");
			if ((fragment == null) || (offset == null)) {
				r.httpResponse("404 Not Found", head, "requires uin=... and offset=...");
				return true;
			}
			long uin = UINUtils.valueOf(fragment);
			if (!UINUtils.isRegularUser(uin)) {
				r.httpResponse("404 Not Found", head, "uin " + uin + " not of a regular user");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			je.arrayStart();
			INatsueUserData cache = null;
			LinkedList<NatsueDBWorldInfo> wir = database.getWorldsInUser(UINUtils.uid(uin), PAGE_SIZE_API_USER_WORLDS, Integer.parseInt(offset));
			if (wir != null) {
				for (NatsueDBWorldInfo wi : wir) {
					// all of these worlds should have one user in common
					cache = Resources.encodeWorld(je, wi, hub, cache);
				}
			}
			je.arrayEnd();
			r.httpOk(head, "application/json", je.out.toString());
		} else if (url.equals("/api/creaturePhotoIndices")) {
			String moniker = qv.get("moniker");
			if (moniker == null) {
				r.httpResponse("404 Not Found", head, "requires moniker=...");
				return true;
			}
			JSONEncoder je = new JSONEncoder();
			je.arrayStart();
			for (Integer i : photoStorage.getIndices(moniker))
				je.write(i);
			je.arrayEnd();
			r.httpOk(head, "application/json", je.out.toString());
			return true;
		} else if (url.equals("/api/creaturePhotoMetadata")) {
			String moniker = qv.get("moniker");
			if (moniker == null) {
				r.httpResponse("404 Not Found", head, "requires moniker=...");
				return true;
			}
			String index = qv.get("index");
			if (index == null) {
				r.httpResponse("404 Not Found", head, "requires index=...");
				return true;
			}
			byte[] data = photoStorage.getPhotoMeta(moniker, Integer.parseInt(index));
			if (data == null) {
				r.httpResponse("404 Not Found", head, "no such photo");
				return true;
			}
			r.httpResponse("200 OK", head, "application/json", data);
			return true;
		} else {
			return false;
		}
		return true;
	}
}
