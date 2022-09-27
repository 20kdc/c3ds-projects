/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.hub;

import natsue.server.userdata.INatsueUserData;

/**
 * WWR listener, i.e. knows when people join/leave
 */
public interface IWWRListener {
	/**
	 * For online/offline notifications.
	 * 
	 * Here's the big secret as to how this doesn't cause a race condition:
	 * WWR listeners are called immediately *after* login and *before* logout.
	 * After login, conflict prevention means we essentially hold an exclusive lock on the user until logout.
	 * There's also a requirement for clientLogout that you're not to call it until you're done logging in.
	 * Finally, wwrNotify calls are made specifically during login, once we're out of the synchronized zone.
	 * (Performing the WWR listeners during a synchronized zone is a quick road to deadlock.)
	 * Therefore:
	 * On login, all WWR listeners are done before it's possible to logout.
	 * On logout, all WWR listeners are done before it's possible to log back in.
	 */
	void wwrNotify(boolean online, INatsueUserData userData);
}
