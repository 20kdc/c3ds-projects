/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;

import natsue.config.Config;
import natsue.data.babel.BabelShortUserData;
import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.hli.ChatColours;
import natsue.data.hli.StandardMessages;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.database.NatsueDBUserInfo;
import natsue.server.hubapi.IHubClient;
import natsue.server.hubapi.IHubPrivilegedClientAPI;
import natsue.server.hubapi.IHubPrivilegedAPI.MsgSendType;
import natsue.server.system.cmd.BaseBotCommand;
import natsue.server.system.cmd.BaseBotCommand.Cat;
import natsue.server.system.cmd.BaseBotCommand.Context;
import natsue.server.userdata.INatsueUserData;

/**
 * This client represents a user called System meant to handle fancy tasks.
 */
public class SystemUserHubClient implements IHubClient, ILogSource {
	public final IHubPrivilegedClientAPI hub;
	private final ILogProvider logParent;
	public static final long UIN = UINUtils.SERVER_UIN;
	public static final INatsueUserData.Root IDENTITY = new INatsueUserData.Fixed(new BabelShortUserData("", "", "!System", UIN), FLAG_RECEIVE_NB_NORNS | FLAG_NO_RANDOM);
	public final int maxDecompressedPRAYSize;
	public final HashMap<String, BaseBotCommand> botCommands = new HashMap<>();

	public SystemUserHubClient(Config config, ILogProvider log, IHubPrivilegedClientAPI h) {
		hub = h;
		logParent = log;
		maxDecompressedPRAYSize = config.maxDecompressedPRAYSize.getValue();
		for (BaseBotCommand command : SystemCommands.commands)
			addBotCommand(command);
	}

	private void addBotCommand(BaseBotCommand command) {
		botCommands.put(command.name, command);
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	@Override
	public INatsueUserData.Root getUserData() {
		return IDENTITY;
	}

	@Override
	public boolean isSystem() {
		return true;
	}

	@Override
	public boolean forceDisconnect(boolean sync) {
		// You can't disconnect !System, that'd be absurd
		return false;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData theirData) {
		if (online)
			hub.sendMessage(theirData.getUIN(), StandardMessages.addToContactList(theirData.getUIN(), UIN), MsgSendType.Temp);
	}

	@Override
	public void incomingMessage(PackedMessage message, Runnable reject) {
		if (message instanceof PackedMessagePRAY) {
			try {
				LinkedList<PRAYBlock> info = ((PackedMessagePRAY) message).messageBlocks;
				// Detect creatures we're about to lose
				for (PRAYBlock pb : info) {
					if (pb.getType().equals("GLST")) {
						// Trapped creature - RETURN TO SENDER IMMEDIATELY
						hub.rejectMessage(UIN, message, "!System isn't accepting creatures");
						return;
					}
				}
				// No? Ok, is it chat?
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
							PackedMessage npm = StandardMessages.acceptChatRequest(UIN, getNickname(), pt.strMap.get("ChatID"));
							hub.sendMessage(message.senderUIN, npm, MsgSendType.Temp);
						}
					} else if (chatType.equals("CHAT")) {
						PRAYTags pt = new PRAYTags();
						pt.read(chatMaybe.data);
						String chatID = pt.strMap.get("ChatID");
						String text = pt.strMap.get("Chat Message");
						if ((chatID != null) && (text != null)) {
							BaseBotCommand.Context ctx = new BaseBotCommand.Context(hub, message.senderUIN, text);
							handleCommand(ctx);
							sendChatMessage(message.senderUIN, chatID, ctx.response.toString());
						}
					} else if (chatType.equals("MESG")) {
						PRAYTags pt = new PRAYTags();
						pt.read(chatMaybe.data);
						String subject = pt.strMap.get("Subject");
						String msg = pt.strMap.get("Message");
						if ((subject != null) && (msg != null)) {
							if (subject.equalsIgnoreCase("SYSTEM MSG")) {
								if (hub.isUINAdmin(message.senderUIN)) {
									for (INatsueUserData sud : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
										hub.sendMessage(sud.getUIN(), StandardMessages.systemMessage(sud.getUIN(), msg), MsgSendType.Temp);
									}
								} else {
									hub.rejectMessage(UIN, message, "Have to be admin");
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				log(ex);
			}
		}
	}

	private void handleCommand(Context ctx) {
		if (!ctx.remaining()) {
			ctx.response.append("What command?\n");
		} else {
			String cmd = ctx.nextArg();
			BaseBotCommand cmdI = botCommands.get(cmd);
			if (cmdI == null) {
				ctx.response.append("Unknown command. Try 'help'\n");
			} else if ((cmdI.category == Cat.Admin) && !hub.isUINAdmin(ctx.senderUIN)) {
				ctx.response.append("You're not allowed to do that!\n");
			} else {
				cmdI.run(ctx);
			}
		}
	}

	private void sendChatMessage(long targetUIN, String chatID, String text) {
		hub.sendMessage(targetUIN, StandardMessages.chatMessage(UIN, getNickname(), chatID, text), MsgSendType.Temp);
	}
}
