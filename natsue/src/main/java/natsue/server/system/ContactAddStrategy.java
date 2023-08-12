/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.system;

/**
 * Strategy for adding contacts via the contact command.
 */
public enum ContactAddStrategy {
	// Sends the message that adds the contact, and tells user that the contact is being added.
	loud,
	// Doesn't tell user.
	silent,
	// Requires the user to reconnect and stuffs the contact add message into their inbox.
	// So the theory goes, the engine handles contacts sent on connect decently well.
	reconnect
}
