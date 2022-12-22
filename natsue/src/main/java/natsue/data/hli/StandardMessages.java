/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.hli;

import java.util.Random;

import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.data.babel.pm.PackedMessageWrit;
import natsue.data.pray.PRAYBlock;
import natsue.data.pray.PRAYTags;

/**
 * For interacting with the default Bootstrap.
 */
public class StandardMessages {
	private static final Random generator = new Random();

	/**
	 * Adds someone to the contact book.
	 * Must be sent "as if it came from the target themselves", so pass in their UIN.
	 */
	public static PackedMessage addToContactList(long targetUIN, long contactUIN) {
		return new PackedMessageWrit(targetUIN, "add_to_contact_book", 2468, UINUtils.toString(contactUIN), null);
	}

	/**
	 * Opens a text dialog announcement.
	 * Must be sent "as if it came from the target themselves", so pass in their UIN.
	 */
	public static PackedMessage systemMessage(long targetUIN, String text) {
		return new PackedMessageWrit(targetUIN, "system_message", 2469, text, null);
	}

	/**
	 * A chat request.
	 */
	public static PackedMessage chatRequest(long senderUIN, String nickname, String chatID) {
		PRAYTags res = new PRAYTags();
		res.strMap.put("Sender UserID", UINUtils.toString(senderUIN));
		// vanilla really does NOT care, but it *does* read this field in for some reason
		res.strMap.put("Date Sent", "19551101000000");
		res.strMap.put("Request Type", "Request");
		res.strMap.put("ChatID", chatID);
		res.strMap.put("Sender Nickname", nickname);
		return tagsMessage(senderUIN, "REQU", res);
	}

	/**
	 * Accept chat request.
	 */
	public static PackedMessage acceptChatRequest(long senderUIN, String nickname, String chatID) {
		PRAYTags res = new PRAYTags();
		res.strMap.put("Sender UserID", UINUtils.toString(senderUIN));
		res.strMap.put("ChatID", chatID);
		res.strMap.put("Request Type", "Accept");
		res.strMap.put("Sender Nickname", nickname);
		return tagsMessage(senderUIN, "REQU", res);
	}

	/**
	 * Chat message.
	 */
	public static PackedMessage chatMessage(long senderUIN, String senderNick, String chatID, String text) {
		PRAYTags res = new PRAYTags();
		res.strMap.put("Chat Message Type", "Message");
		res.strMap.put("Sender UserID", UINUtils.toString(senderUIN));
		res.strMap.put("ChatID", chatID);
		res.strMap.put("Sender Nickname", senderNick);
		res.strMap.put("Chat Message", text);
		return tagsMessage(senderUIN, "CHAT", res);
	}

	/**
	 * Leaving chat.
	 */
	public static PackedMessage chatLeave(long senderUIN, String senderNick, String chatID, long targetUIN, String targetNick) {
		PRAYTags res = new PRAYTags();
		res.strMap.put("ChatID", chatID);
		res.strMap.put("Sender Nickname", senderNick);
		res.strMap.put("Sender UserID", UINUtils.toString(senderUIN));
		res.strMap.put("Chat Message Type", "Chatter go Bye Bye");
		res.strMap.put("Exiting Chatter Nickname", targetNick);
		res.strMap.put("Exiting Chatter UserID", UINUtils.toString(targetUIN));
		return tagsMessage(senderUIN, "CHAT", res);
	}

	/**
	 * Generates a PRAY block name.
	 */
	public static String generateBlockName(String type) {
		long randomRes = generator.nextLong();
		return "natsue_" + randomRes + "_intgen_" + System.currentTimeMillis() + "_" + type;
	}

	/**
	 * Arbitrary PRAY tags message.
	 */
	public static PackedMessage tagsMessage(long senderUIN, String type, PRAYTags tags) {
		PRAYBlock pb = new PRAYBlock(type, generateBlockName(type), tags.toByteArray());
		return new PackedMessagePRAY(senderUIN, pb);
	}
}
