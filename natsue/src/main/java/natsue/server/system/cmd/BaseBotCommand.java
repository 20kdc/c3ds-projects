/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system.cmd;

import java.util.List;

import natsue.data.TOTP;
import natsue.data.babel.UINUtils;
import natsue.data.hli.ChatColours;
import natsue.log.ILogSource;
import natsue.server.hubapi.IHubPrivilegedClientAPI;
import natsue.server.hubapi.IHubClientAsSeenByOtherClientsPrivileged;
import natsue.server.userdata.INatsueUserData;
import natsue.server.userdata.INatsueUserData.LongTermPrivileged;

/**
 * The base for all System commands.
 */
public abstract class BaseBotCommand {
	public final String name, helpArgs, helpSummary, helpText, helpExample;
	public final Cat category;

	public BaseBotCommand(String n, String ha, String hs, String ht, String he, Cat ao) {
		name = n;
		helpArgs = ha;
		helpSummary = hs;
		helpText = ht;
		helpExample = he;
		category = ao;
	}
	public abstract void run(Context args);

	public static enum Cat {
		Public(false, false),
		Public2FA(false, true),
		Secret(false, false),
		Admin(true, false),
		Admin2FA(true, true),
		Research(true, false);

		public final boolean requiresAdmin, requires2FA;

		Cat(boolean ra, boolean ra2) {
			requiresAdmin = ra;
			requires2FA = ra2;
		}
	}

	public static class Context {
		/**
		 * Hub/etc.
		 */
		public final IHubPrivilegedClientAPI hub;

		/**
		 * Command sender (as IUserConnectionInfo).
		 */
		public final IHubClientAsSeenByOtherClientsPrivileged sender;

		/**
		 * Command sender.
		 */
		public final long senderUIN;

		/**
		 * Response buffer.
		 */
		public final StringBuilder response = new StringBuilder();

		/**
		 * If the response message is inhibited.
		 */
		public boolean responseInhibited = false;

		/**
		 * Accumulated text.
		 */
		public final char[] text;

		/**
		 * Current position in the text array.
		 */
		public int index = 0;

		/**
		 * Log source for commands that log stuff.
		 */
		public final ILogSource log;

		/**
		 * Information for the help command.
		 */
		public final List<BaseBotCommand> helpInfo;

		public Context(IHubPrivilegedClientAPI hub, IHubClientAsSeenByOtherClientsPrivileged s, String tex, ILogSource lSrc, List<BaseBotCommand> hi) {
			log = lSrc;
			response.append(ChatColours.CHAT);
			this.hub = hub;
			sender = s;
			senderUIN = s.getUIN();
			helpInfo = hi;
			// strip initial tint
			if (tex.contains(">"))
				tex = tex.substring(tex.indexOf('>') + 1);
			tex = tex.trim();
			// and confirm
			text = tex.toCharArray();
		}

		/**
		 * Just eats all whitespace.
		 */
		private void consumeWhitespace() {
			while (index < text.length) {
				if (text[index] <= 32) {
					index++;
				} else {
					break;
				}
			}
		}

		/**
		 * Just eats all text.
		 */
		private void consumeText() {
			while (index < text.length) {
				if (text[index] > 32) {
					index++;
				} else {
					break;
				}
			}
		}

		/**
		 * Returns true if there are remaining arguments.
		 */
		public boolean remaining() {
			consumeWhitespace();
			return index < text.length;
		}

		/**
		 * Gets the next arg, or null if none!
		 */
		public String nextArg() {
			consumeWhitespace();
			if (index == text.length)
				return null;
			int startIndex = index;
			consumeText();
			return new String(text, startIndex, index - startIndex);
		}

		/**
		 * Returns all characters to the end of the line.
		 */
		public String toEnd() {
			consumeWhitespace();
			return new String(text, index, text.length - index);
		}

		public void appendNoSuchUser(String user) {
			response.append(ChatColours.CHAT);
			response.append("'");
			response.append(ChatColours.NICKNAME);
			response.append(user);
			response.append(ChatColours.CHAT);
			response.append("' doesn't exist. Specify a nickname or UIN.\n");
		}

		public INatsueUserData.LongTermPrivileged commandLookupUserLongTerm(String ref) {
			long asUIN = UINUtils.valueOf(ref);
			if (asUIN != -1)
				return hub.openUserDataByUINLT(asUIN);
			return hub.openUserDataByNicknameLT(ref);
		}

		public INatsueUserData commandLookupUser(String ref) {
			long asUIN = UINUtils.valueOf(ref);
			if (asUIN != -1)
				return hub.getUserDataByUIN(asUIN);
			return hub.getUserDataByNickname(ref);
		}

		public void appendNewPassword(String newPW, LongTermPrivileged userData) {
			response.append("Reset password to: " + newPW + "\n");
			byte[] newSecret = userData.calculate2FASecret(newPW);
			if (newSecret != null) {
				try {
					response.append("2FA secret: " + new String(TOTP.encodeBase32(newSecret)) + "\n");
				} catch (TOTP.InvalidTOTPKeyException ex) {
					// shouldn't be possible
					throw new RuntimeException(ex);
				}
			}
		}
	}
}
