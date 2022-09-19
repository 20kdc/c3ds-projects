/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import java.util.LinkedList;
import java.util.Random;

import natsue.config.Config;
import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.PackedMessage;
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;
import natsue.data.babel.WritVal;
import natsue.data.hli.StandardMessages;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.hubapi.IHubClient;
import natsue.server.hubapi.IHubPrivilegedClientAPI;

/**
 * This client represents a user called System meant to handle fancy tasks.
 */
public class SystemUserHubClient implements IHubClient, ILogSource {
	public final IHubPrivilegedClientAPI hub;
	private final ILogProvider logParent;
	public final BabelShortUserData userData = new BabelShortUserData("", "", "!System", UINUtils.SERVER_UIN);
	public final int maxDecompressedPRAYSize;

	private final Random random = new Random();
	private final Object randomLock = new Object();

	private final String COL_NICKNAME = "<tint 120 220 250>";
	private final String COL_CHAT = "<tint 96 160 192>";

	public SystemUserHubClient(Config config, ILogProvider log, IHubPrivilegedClientAPI h) {
		hub = h;
		logParent = log;
		maxDecompressedPRAYSize = config.maxDecompressedPRAYSize.getValue();
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	@Override
	public BabelShortUserData getUserData() {
		return userData;
	}

	@Override
	public boolean isSystem() {
		return true;
	}

	@Override
	public void wwrNotify(boolean online, BabelShortUserData theirData) {
		hub.sendMessage(theirData.uin, StandardMessages.addToContactList(theirData.uin, userData.uin), true);
		/*
		writ = WritVal.encodeWrit("system_message", 2469, "You didn't say the magic word!", null);
		try {
			hub.forceRouteMessage(theirData.uin, new PackedMessage(theirData.uin, PackedMessage.TYPE_WRIT, writ));
		} catch (Exception ex) {
			logTo(hub.log, ex);
		}*/
	}

	@Override
	public void incomingMessage(PackedMessage message, Runnable reject) {
		if (message.messageType == PackedMessage.TYPE_PRAY) {
			try {
				LinkedList<PRAYBlock> info = PRAYBlock.read(PacketReader.wrapLE(message.messageData), maxDecompressedPRAYSize);
				if (info.size() == 1) {
					PRAYBlock chatMaybe = info.getFirst();
					String chatType = chatMaybe.getType();
					if (chatType.equals("REQU")) {
						// Chat request to System?
						PRAYTags pt = new PRAYTags();
						pt.read(chatMaybe.data);
						String str = pt.strMap.get("Request Type");
						if (str.equals("Request")) {
							// Yes - we need to accept.
							PRAYTags res = new PRAYTags();
							String myUINStr = UINUtils.toString(userData.uin);
							res.strMap.put("Sender UserID", myUINStr);
							res.strMap.put("Date Sent", pt.strMap.get("Date Sent"));
							res.strMap.put("ChatID", pt.strMap.get("ChatID"));
							res.strMap.put("Request Type", "Accept");
							res.strMap.put("Sender Nickname", userData.nickName);
							sendTagsMessage(message.senderUIN, "REQU", res.toByteArray());
						}
					} else if (chatType.equals("CHAT")) {
						PRAYTags pt = new PRAYTags();
						pt.read(chatMaybe.data);
						String chatID = pt.strMap.get("ChatID");
						String text = pt.strMap.get("Chat Message");
						if ((chatID != null) && (text != null)) {
							// strip initial tint
							if (text.contains(">"))
								text = text.substring(text.indexOf('>') + 1);
							text = text.trim();
							handleChatMessage(message.senderUIN, chatID, text);
						}
					}
				}
			} catch (Exception ex) {
				log(ex);
			}
		}
	}

	private BabelShortUserData commandLookupUser(String ref) {
		int plusIdx = ref.indexOf('+');
		if (plusIdx >= 0) {
			try {
				String a = ref.substring(0, plusIdx);
				String b = ref.substring(plusIdx + 1);
				return hub.getShortUserDataByUIN(UINUtils.make(Integer.valueOf(a), Integer.valueOf(b)));
			} catch (Exception ex) {
				return null;
			}
		}
		return hub.getShortUserDataByNickname(ref);
	}

	private void handleChatMessage(long targetUIN, String chatID, String text) {
		StringBuilder response = new StringBuilder();
		if (text.startsWith("whois ")) {
			String user = text.substring(6);
			BabelShortUserData userData = commandLookupUser(user);
			if (userData != null) {
				response.append(userData.nickName);
				response.append(COL_CHAT);
				response.append(" - ");
				if (hub.isUINOnline(userData.uin)) {
					response.append("<tint 64 255 64>Online\n");
				} else {
					response.append("<tint 255 64 64>Offline\n");
				}
				response.append(COL_CHAT);
				response.append("UIN: ");
				response.append(UINUtils.toString(userData.uin));
				if (UINUtils.hid(userData.uin) == UINUtils.HID_SYSTEM) {
					response.append(" <tint 64 64 255>(SYSTEM)\n");
				} else if (UINUtils.hid(userData.uin) == UINUtils.HID_USER) {
					response.append(" <tint 64 255 64>(USER)\n");
				} else {
					response.append(" <tint 255 64 64>(ERROR)\n");
				}
			} else {
				appendNoSuchUser(response, user);
			}
		} else if (text.startsWith("contact ")) {
			String user = text.substring(8);
			BabelShortUserData userData = commandLookupUser(user);
			if (userData != null) {
				response.append(COL_CHAT);
				response.append("Adding to your contact list...\n");
				hub.sendMessage(targetUIN, StandardMessages.addToContactList(targetUIN, userData.uin), true);
			} else {
				appendNoSuchUser(response, user);
			}
		} else if (text.equals("who")) {
			boolean first = true;
			for (BabelShortUserData data : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
				if (!first) {
					response.append(COL_CHAT);
					response.append(", ");
					response.append(COL_NICKNAME);
				}
				response.append(data.nickName);
				first = false;
			}
			response.append("\n");
		} else if (text.equals("whoami")) {
			response.append(COL_CHAT);
			response.append("A philosophical question.\n");
			response.append("To me? You are " + UINUtils.toString(targetUIN) + ".\n");
			response.append("Others may say differently.\n");
		} else {
			response.append(COL_CHAT);
			response.append("Unknown command. Try:\nwhois !System\ncontact !System\nwho (show who's online)\n");
		}
		sendChatMessage(targetUIN, chatID, response.toString());
	}

	private void appendNoSuchUser(StringBuilder response, String user) {
		response.append(COL_CHAT);
		response.append("'");
		response.append(COL_NICKNAME);
		response.append(user);
		response.append(COL_CHAT);
		response.append("' doesn't exist. Specify a nickname or UIN.\n");
	}

	private void sendChatMessage(long targetUIN, String chatID, String text) {
		// uuuh
		PRAYTags res = new PRAYTags();
		res.strMap.put("Chat Message Type", "Message");
		res.strMap.put("Sender UserID", UINUtils.toString(userData.uin));
		res.strMap.put("ChatID", chatID);
		res.strMap.put("Sender Nickname", userData.nickName);
		res.strMap.put("Chat Message", text);
		sendTagsMessage(targetUIN, "CHAT", res.toByteArray());
	}

	private void sendTagsMessage(long senderUIN, String type, byte[] res) {
		long randomRes;
		synchronized (randomLock) {
			randomRes = random.nextLong();
		}
		byte[] resFile = PRAYBlock.writeFileWithOneBlock(new PRAYBlock(type, "STM_" + randomRes + "_sysrsp", res));
		hub.sendMessage(senderUIN, new PackedMessage(userData.uin, PackedMessage.TYPE_PRAY, resFile), true);
	}
}
