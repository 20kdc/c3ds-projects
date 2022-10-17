/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.packet;

import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import natsue.config.ConfigConnectionQuotas;

/**
 * Manages connection quotas.
 */
public class QuotaManager {
	private HashMap<InetAddress, IPDetail> informationAbout = new HashMap<>();
	public final ConfigConnectionQuotas config;

	public QuotaManager(ConfigConnectionQuotas cq) {
		config = cq;
		new Thread("QuotaManager Timer Daemon") {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						break;
					}
					minuteUpdate();
				}
			}
		}.start();
	}

	private synchronized void minuteUpdate() {
		HashSet<InetAddress> remove = new HashSet<>();
		for (Map.Entry<InetAddress, IPDetail> ina : informationAbout.entrySet()) {
			IPDetail ipd = ina.getValue();
			ipd.connectionsLastMinute = ipd.connectionsThisMinute;
			ipd.connectionsThisMinute = 0;
			if (ipd.currentConnections == 0)
				if (ipd.connectionsLastMinute == 0)
					remove.add(ina.getKey());
		}
		for (InetAddress ia : remove)
			informationAbout.remove(ia);
	}

	public synchronized boolean socketStart(Socket skt) {
		IPDetail ipd = informationAbout.get(skt.getInetAddress());
		if (ipd == null)
			informationAbout.put(skt.getInetAddress(), ipd = new IPDetail());
		if (ipd.connectionsThisMinute + ipd.connectionsLastMinute >= config.maxConnectionsInTwoMinutes.getValue())
			return false;
		if (ipd.currentConnections >= config.maxConnectionsConcurrent.getValue())
			return false;
		ipd.connectionsThisMinute++;
		ipd.currentConnections++;
		return true;
	}

	public synchronized void socketEnd(Socket skt) {
		IPDetail ipd = informationAbout.get(skt.getInetAddress());
		if (ipd != null)
			ipd.currentConnections--;
	}

	public synchronized void runSystemCheck(StringBuilder sb) {
		sb.append("QuotaManager\n");
		for (Map.Entry<InetAddress, IPDetail> ipd : informationAbout.entrySet()) {
			sb.append(ipd.getKey().toString());
			sb.append("\n C");
			sb.append(ipd.getValue().currentConnections);
			sb.append(" T");
			sb.append(ipd.getValue().connectionsThisMinute);
			sb.append(" L");
			sb.append(ipd.getValue().connectionsLastMinute);
			sb.append('\n');
		}
	}

	public static class IPDetail {
		public int currentConnections;
		public int connectionsLastMinute;
		public int connectionsThisMinute;
	}
}
