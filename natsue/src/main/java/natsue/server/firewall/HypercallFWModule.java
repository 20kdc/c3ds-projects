/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import cdsp.common.data.pray.PRAYBlock;
import cdsp.common.data.pray.PRAYTags;
import natsue.config.Config;
import natsue.data.babel.PacketReader;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.babel.pm.PackedMessageWrit;
import natsue.data.hli.StandardMessages;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.hubapi.IHubPrivilegedAPI;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.userdata.INatsueUserData;

/**
 * NET: WRIT translator
 */
public class HypercallFWModule implements IFWModule, ILogSource {
	public static final int HVAPI_VERSION = 1;

	public final IHubPrivilegedAPI hub;
	private final ILogProvider logParent;
	public final Config config;

	public HypercallFWModule(ILogProvider lp, IHubPrivilegedAPI h, Config config) {
		hub = h;
		logParent = lp;
		this.config = config;
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public boolean handleMessage(INatsueUserData sourceUser, INatsueUserData destUser, PackedMessage message) {
		if (message instanceof PackedMessagePRAY) {
			if (((PackedMessagePRAY) message).messageBlocks.size() == 1) {
				PRAYBlock res = ((PackedMessagePRAY) message).messageBlocks.getFirst();
				if (res.getType().equals("N@SU") && res.getName().equals("natsue_hypercall")) {
					try {
						// Yes, this is a hypercall.
						PRAYTags pt = new PRAYTags(PacketReader.CHARSET);
						pt.read(res.data);
						String type = pt.strMap.get("Type");
						if (type == null) {
							hypercallError(sourceUser.getUIN(), "Hypercall without type.");
							return true;
						}
						if (type.equals("writ")) {
							String channel = pt.strMap.get("Channel");
							if (channel == null) {
								hypercallError(sourceUser.getUIN(), "WRIT without channel");
								return true;
							}
							int writMsg = pt.intMap.getOrDefault("Message", 2468);
							Object writParam1 = getWritParam(pt, "Param1 ");
							Object writParam2 = getWritParam(pt, "Param2 ");
							PackedMessageWrit pmw = new PackedMessageWrit(sourceUser.getUIN(), channel, writMsg, writParam1, writParam2);
							// don't allow hypercall writs to bypass the writ size
							if (pmw.determineSize() > config.messages.maxNetWritSize.getValue()) {
								hypercallError(sourceUser.getUIN(), "WRIT too large");
								return true;
							}
							hub.impGiveMessage(sourceUser, destUser.getUIN(), pmw);
						} else {
							// Unknown
							hypercallError(sourceUser.getUIN(), "Unknown hypercall '" + type + "'. Outdated server?");
						}
					} catch (Exception ex) {
						log(ex);
						hypercallError(sourceUser.getUIN(), "Internal error processing hypercall");
					}
					return true;
				}
			}
		}
		return false;
	}

	private Object getWritParam(PRAYTags pt, String pfx) {
		String s1 = pt.strMap.get(pfx + "String");
		if (s1 != null)
			return s1;
		String s2 = pt.strMap.get(pfx + "Float");
		if (s2 != null) {
			try {
				return Float.parseFloat(s2);
			} catch (Exception ex) {
			}
		}
		Integer s3 = pt.intMap.get(pfx + "Int");
		if (s3 != null)
			return s3;
		return null;
	}

	private void hypercallError(long uin, String err) {
		err = "NATSUE HYPERCALL INTERFACE:\n" + err;
		hub.sendMessage(uin, StandardMessages.systemMessage(uin, err), MsgSendType.Temp, uin);
	}
}
