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
import java.util.HashSet;
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
	public static final String CHATID_GLOBAL = "pettables (19551101000000) - 1+2";

	public final IHubPrivilegedClientAPI hub;
	private final ILogProvider logParent;
	public static final long UIN = UINUtils.SERVER_UIN;
	public static final INatsueUserData.Root IDENTITY = new INatsueUserData.Fixed(new BabelShortUserData("", "", "!System", UIN), FLAG_RECEIVE_NB_NORNS | FLAG_NO_RANDOM);
	public final int maxDecompressedPRAYSize;
	public final HashMap<String, BaseBotCommand> botCommands = new HashMap<>();

	// Synchronized by the following lock
	public final HashSet<Long> peopleInGroupChat = new HashSet<>();
	public final Object peopleInGroupChatLock = new Object();

	public SystemUserHubClient(Config config, ILogProvider log, IHubPrivilegedClientAPI h) {
		hub = h;
		logParent = log;
		maxDecompressedPRAYSize = config.maxDecompressedPRAYSize.getValue();
		for (BaseBotCommand command : SystemCommands.commands)
			addBotCommand(command);
		// these commands in particular care about our hub client
		addBotCommand(new BaseBotCommand("globalchat", "", BaseBotCommand.Cat.Public) {
			@Override
			public void run(Context args) {
				if (args.remaining()) {
					args.response.append("This command doesn't take any parameters - it will make a chat request.\n");
					return;
				}
				// check the blatantly obvious before sending an invite
				boolean hasMe;
				synchronized (peopleInGroupChatLock) {
					hasMe = peopleInGroupChat.contains(args.senderUIN);
				}
				if (hasMe) {
					args.response.append("Already in global chat (reconnect?)\n");
				} else {
					args.response.append("Please stand by.\n");
					sendGlobalChatRequest(args.senderUIN);
				}
			}
		});
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
		if (online) {
			hub.sendMessage(theirData.getUIN(), StandardMessages.addToContactList(theirData.getUIN(), UIN), MsgSendType.Temp);
		} else {
			removeFromGlobalChat(theirData.getUIN(), "disconnected");
		}
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
						String chatID = pt.strMap.get("ChatID");
						if ((str != null) && (chatID != null)) {
							if (str.equals("Request")) {
								// Yes - we need to accept.
								PackedMessage npm = StandardMessages.acceptChatRequest(UIN, getNickname(), pt.strMap.get("ChatID"));
								hub.sendMessage(message.senderUIN, npm, MsgSendType.Temp);
							} else if (str.equals("Accept")) {
								if (chatID.equals(CHATID_GLOBAL)) {
									addToGlobalChat(message.senderUIN);
								}
							}
						}
					} else if (chatType.equals("CHAT")) {
						PRAYTags pt = new PRAYTags();
						pt.read(chatMaybe.data);
						String cmt = pt.strMap.get("Chat Message Type");
						String chatID = pt.strMap.get("ChatID");
						if ((chatID != null) && (cmt != null)) {
							if (cmt.equals("Message")) {
								String text = pt.strMap.get("Chat Message");
								if (text != null)
									handleChatMessage(message.senderUIN, chatID, text);
							} else if (cmt.equals("Chatter go Bye Bye")) {
								if (chatID.equals(CHATID_GLOBAL))
									removeFromGlobalChat(message.senderUIN, "left");
							}
						}
					} else if (chatType.equals("MESG")) {
						PRAYTags pt = new PRAYTags();
						pt.read(chatMaybe.data);
						String subject = pt.strMap.get("Subject");
						String msg = pt.strMap.get("Message");
						if ((subject != null) && (msg != null))
							handleSnailMessage(message, subject, msg);
					}
				}
			} catch (Exception ex) {
				log(ex);
			}
		}
	}

	private void handleSnailMessage(PackedMessage packed, String subject, String msg) {
		if ((subject != null) && (msg != null)) {
			if (subject.equalsIgnoreCase("SYSTEM MSG")) {
				if (hub.isUINAdmin(packed.senderUIN)) {
					for (INatsueUserData sud : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
						hub.sendMessage(sud.getUIN(), StandardMessages.systemMessage(sud.getUIN(), msg), MsgSendType.Temp);
					}
				} else {
					hub.rejectMessage(UIN, packed, "Have to be admin");
				}
			}
		}
	}

	private void handleChatMessage(long senderUIN, String chatID, String text) {
		if (chatID.equals(CHATID_GLOBAL)) {
			// Need this to get the nickname.
			INatsueUserData nud = hub.getUserDataByUIN(senderUIN);
			if (nud == null)
				return;
			String senderNick = nud.getNickname();
			if (!sendToGlobalChatExcept(senderUIN, senderNick, text)) {
				sendChatMessage(senderUIN, chatID, "Error: not in global chat.\nAttempting to correct.\n");
				// Just remove EVERYBODY who could have screwed this up. Reset things.
				for (INatsueUserData potential : hub.listAllNonSystemUsersOnlineYesIMeanAllOfThem()) {
					if (potential.getUIN() != senderUIN) {
						PackedMessage pm = StandardMessages.chatLeave(potential.getUIN(), potential.getNickname(), CHATID_GLOBAL, potential.getUIN(), potential.getNickname());
						hub.sendMessage(senderUIN, pm, MsgSendType.Temp);
						pm = StandardMessages.chatLeave(senderUIN, senderNick, CHATID_GLOBAL, senderUIN, senderNick);
						hub.sendMessage(potential.getUIN(), pm, MsgSendType.Temp);
					}
				}
				addToGlobalChat(senderUIN);
			}
		} else {
			BaseBotCommand.Context ctx = new BaseBotCommand.Context(hub, senderUIN, text);
			handleCommand(ctx);
			sendChatMessage(senderUIN, chatID, ctx.response.toString());
		}
	}

	private void sendGlobalChatRequest(long targetUIN) {
		hub.sendMessage(targetUIN, StandardMessages.chatRequest(UIN, getNickname(), CHATID_GLOBAL), MsgSendType.Temp);
	}
	
	private void addToGlobalChat(long senderUIN) {
		synchronized (peopleInGroupChatLock) {
			peopleInGroupChat.add(senderUIN);
		}
		String leader = "<tint 255 255 255> - GLOBAL CHAT -\n" + ChatColours.CHAT + "Other chatters invisible due to quirks of the Docking Station chat system. Please DO NOT invite users, tell them to use the globalchat !System command.\n";
		PackedMessage npm = StandardMessages.chatMessage(UIN, "", CHATID_GLOBAL, leader);
		hub.sendMessage(senderUIN, npm, MsgSendType.Temp);
		sendGlobalChatStatusUpdate(senderUIN, "joined");
	}

	private void removeFromGlobalChat(long targetUIN, String cause) {
		boolean hadToRemove = false;
		synchronized (peopleInGroupChatLock) {
			hadToRemove = peopleInGroupChat.remove(targetUIN);
		}
		if (hadToRemove) {
			sendGlobalChatStatusUpdate(targetUIN, cause);
		}
	}

	private void sendGlobalChatStatusUpdate(long targetUIN, String status) {
		String quickNick = "!404";
		INatsueUserData nud = hub.getUserDataByUIN(targetUIN);
		if (nud != null)
			quickNick = nud.getNickname();
		sendToGlobalChatExcept(0, "", ChatColours.NICKNAME + quickNick + ChatColours.CHAT + " " + status + ".\n");
	}

	private boolean sendToGlobalChatExcept(long senderUIN, String nickname, String text) {
		LinkedList<Long> targets;
		synchronized (peopleInGroupChatLock) {
			targets = new LinkedList<>(peopleInGroupChat);
		}
		// can't talk to the global chat you aren't in
		if (senderUIN != 0)
			if (!targets.remove(senderUIN))
				return false;
		// ok, so this is some high-level abuse of client jank here
		// basically the client doesn't care if your nickname is wrong, it will just write whatever you put in
		// that in mind, global chat is just a matter of doing the thing
		for (Long target : targets)
			hub.sendMessage(target, StandardMessages.chatMessage(UIN, nickname, CHATID_GLOBAL, text), MsgSendType.Temp);
		return true;
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
