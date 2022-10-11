/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.http;

import java.io.IOException;
import java.util.LinkedList;

import natsue.data.babel.UINUtils;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.INatsueUserFlags;
import natsue.server.database.NatsueDBCreatureEvent;
import natsue.server.database.NatsueDBCreatureInfo;
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
		if (url.equals("/")) {
			StringBuilder rsp = new StringBuilder();
			rsp.append("<h1>" + SystemCommands.VERSION + "</h1>\n");
			rsp.append("<marquee>Welcome to Natsue Server!</marquee>\n");
			rsp.append("Current online users:<ul>\n");
			for (INatsueUserData nud : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
				rsp.append("<li>");
				rsp.append("<a href=\"user?" + HTMLEncoder.urlEncode(nud.getNickname()) + "\">");
				HTMLEncoder.htmlEncode(rsp, nud.getNickname());
				rsp.append("</a>");
				rsp.append("</li>\n");
			}
			rsp.append("</ul>\n");
			r.httpOk(head, "text/html", rsp.toString());
		} else if (url.startsWith("/creature?")) {
			StringBuilder rsp = new StringBuilder();
			String fragment = HTMLEncoder.urlDecode(url.substring(10));
			rsp.append("<h1>" + SystemCommands.VERSION + "</h1>\n");
			rsp.append("<h2>Creature ");
			HTMLEncoder.htmlEncode(rsp, fragment);
			rsp.append("</h2>\n");
			NatsueDBCreatureInfo ci = database.getCreatureInfo(fragment);
			if (ci != null) {
				rsp.append("General Information:<ul>\n");
				rsp.append("<li>Name: ");
				HTMLEncoder.htmlEncode(rsp, ci.name);
				rsp.append("</li>");
				rsp.append("<li>User Text: ");
				HTMLEncoder.htmlEncode(rsp, ci.userText);
				rsp.append("</li>");
				rsp.append("<li>First Seen At UID: ");
				rsp.append(ci.senderUID);
				rsp.append("</li>");
				rsp.append("<li>CH0: ");
				rsp.append(ci.ch0);
				rsp.append("</li>");
				rsp.append("<li>CH1: ");
				rsp.append(ci.ch1);
				rsp.append("</li>");
				rsp.append("<li>CH2: ");
				rsp.append(ci.ch2);
				rsp.append("</li>");
				rsp.append("<li>CH3: ");
				rsp.append(ci.ch3);
				rsp.append("</li>");
				rsp.append("<li>CH4: ");
				rsp.append(ci.ch4);
				rsp.append("</li>");
				rsp.append("</ul>\n");
			} else {
				rsp.append("General Information not available<br/>\n");
			}
			LinkedList<NatsueDBCreatureEvent> ll = database.getCreatureEvents(fragment);
			if (ll != null) {
				rsp.append("Events:<table border=1>\n");
				rsp.append("<tr><td>Sender UID</td><td>Index</td><td>Type</td><td>World Time</td><td>Age</td>\n");
				rsp.append("<td>Unix Time</td><td>Life Stage</td><td>Param 1</td><td>Param 2</td><td>World Name</td>\n");
				rsp.append("<td>World ID</td><td>User ID</td></tr>\n");
				for (NatsueDBCreatureEvent ev : ll) {
					rsp.append("<tr>");
					//
					rsp.append("<td>");
					rsp.append(ev.senderUID);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					rsp.append(ev.eventIndex);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					rsp.append(ev.eventType);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					rsp.append(ev.worldTime);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					rsp.append(ev.ageTicks);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					rsp.append(ev.unixTime);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					rsp.append(ev.lifeStage);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					HTMLEncoder.htmlEncode(rsp, ev.param1);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					HTMLEncoder.htmlEncode(rsp, ev.param2);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					HTMLEncoder.htmlEncode(rsp, ev.worldName);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					HTMLEncoder.htmlEncode(rsp, ev.worldID);
					rsp.append("</td>");
					//
					rsp.append("<td>");
					HTMLEncoder.htmlEncode(rsp, ev.userID);
					rsp.append("</td>");
					//
					rsp.append("</tr>\n");
				}
				rsp.append("</table>\n");
			}
			r.httpOk(head, "text/html", rsp.toString());
		} else if (url.startsWith("/user?")) {
			StringBuilder rsp = new StringBuilder();
			String fragment = HTMLEncoder.urlDecode(url.substring(6));
			INatsueUserData data = hub.getUserDataByNickname(fragment);
			if (data == null)
				data = hub.getUserDataByUIN(UINUtils.valueOf(fragment));
			if (data == null) {
				rsp.append("<i>No such user.</i>");
			} else {
				rsp.append("<h1>" + SystemCommands.VERSION + "</h1>\n");
				rsp.append("<h2>User ");
				HTMLEncoder.htmlEncode(rsp, data.getNickname());
				rsp.append(" (");
				rsp.append(data.getUINString());
				rsp.append(")</h2>\n");
				if (hub.isUINOnline(data.getUIN())) {
					rsp.append("Online<br/>");
				} else {
					rsp.append("Offline<br/>");
				}
				rsp.append("Flags: " + INatsueUserFlags.Flag.showFlags(data.getFlags()) + "<br/>");
			}
			r.httpOk(head, "text/html", rsp.toString());
		} else {
			r.httpResponse("404 Not Found", head, "No such file");
		}
	}
}
