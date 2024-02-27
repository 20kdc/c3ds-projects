/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hubapi;

import java.util.LinkedList;

import natsue.data.babel.pm.PackedMessage;
import natsue.server.cryo.CryoFrontend;
import natsue.server.firewall.IRejector;
import natsue.server.userdata.IHubUserDataCachePrivilegedProxy;
import natsue.server.userdata.INatsueUserData;

/**
 * Represents the server.
 */
public interface IHubPrivilegedAPI extends IHubCommonAPI, IHubUserDataCachePrivilegedProxy, IHubLoginAPI, IRejector {
	/**
	 * Returns all user info.
	 */
	LinkedList<IHubClientAsSeenByOtherClients> listAllUsersOnlineYesIMeanAllOfThem();

	// see base
	IHubClientAsSeenByOtherClientsPrivileged getConnectionByUIN(long uin);

	/**
	 * Adds a client to the system, or returns false if that couldn't happen due to a conflict.
	 * Note that you can't turn back if this returns true, you have to logout again.
	 * The runnable provided here runs at a very specific time such that:
	 * + No functions will quite have been called yet on the client
	 * + The client will definitely be logging in at this point
	 */
	boolean clientLogin(IHubClient client, Runnable confirmOk);

	/**
	 * This is the "impersonation-friendly" give-message endpoint.
	 * CLIENTS SHOULD USE clientGiveMessage instead.
	 * This is for hypercalls, which need to do things on behalf of a client.
	 * Hypercalls shouldn't be allowed to skip the firewall, though, so they must use this.
	 */
	void impGiveMessage(INatsueUserData userData, long destinationUIN, PackedMessage message);

	/**
	 * Route a message that is expected to *eventually* get to the target.
	 * The message is assumed to be authenticated - this is considered to be past the firewall.
	 * If temp is true, the message won't be archived on failure.
	 * If fromRejector is true, then the message won't go through rejection *again*.
	 * causeUIN is used for abuse tracking purposes.
	 */
	void sendMessage(long destinationUIN, PackedMessage message, MsgSendType type, long causeUIN);

	/**
	 * See the other sendMessage definition.
	 * Note that sourceUser is just used as a source for the UIN.
	 * Note also that causeUser is just used as a source for the UIN.
	 */
	default void sendMessage(INatsueUserData destUser, PackedMessage message, MsgSendType type, INatsueUserData causeUser) {
		sendMessage(destUser.getUIN(), message, type, causeUser.getUIN());
	}

	/**
	 * Attempts to forcibly disconnect a user by UIN.
	 * Note that this may not work (system users can shrug it off) but regular users are gone.
	 */
	void forceDisconnectUIN(long uin, boolean sync);

	/**
	 * Properly applies changes to a user's random pool status.
	 */
	void considerRandomStatus(INatsueUserData.LongTerm user);

	/**
	 * Attempts to find anything unusual.
	 */
	String runSystemCheck(boolean detailed);

	/**
	 * Cryo frontend (used by System for cryo-related tasks)
	 */
	CryoFrontend getCryoFE();

	/**
	 * Controls message behaviour.
	 */
	public static enum MsgSendType {
		/**
		 * Chat/etc.
		 * Discarded if target missing.
		 */
		Temp(false, MsgSendFailBehaviour.Discard, true),
		/**
		 * Special message type for system reports
		 * These MUST be decompressed!!!
		 * This allows quick extraction in an emergency.
		 */
		SystemReport(false, MsgSendFailBehaviour.Discard, false),
		/**
		 * Mail/etc.
		 * Will be persisted.
		 */
		Perm(false, MsgSendFailBehaviour.Spool, true),
		/**
		 * Norns.
		 * Will be rejected if target missing.
		 */
		PermReturnIfOffline(false, MsgSendFailBehaviour.Reject, true),
		/**
		 * Rejected temporary message.
		 * These don't exist right now.
		 * (We never have a reason to send these back!)
		 */
		TempReject(true, MsgSendFailBehaviour.Discard, true),
		/**
		 * Rejected permanent message.
		 */
		PermReject(true, MsgSendFailBehaviour.Spool, true);

		public final boolean isReject, compressIfAllowed;
		public final MsgSendFailBehaviour failBehaviour;

		MsgSendType(boolean ir, MsgSendFailBehaviour ss, boolean compressIfAllowed) {
			isReject = ir;
			this.compressIfAllowed = compressIfAllowed;
			failBehaviour = ss;
		}
	}

	/**
	 * Controls behaviour if we accepted the message but we can't actually send it.
	 */
	public static enum MsgSendFailBehaviour {
		// delete
		Discard(true),
		// spool to disk
		Spool(false),
		// return to sender with shiny note
		Reject(false);
		public final boolean allowMessageLoss;
		MsgSendFailBehaviour(boolean mlm) {
			allowMessageLoss = mlm;
		}
	}
}
