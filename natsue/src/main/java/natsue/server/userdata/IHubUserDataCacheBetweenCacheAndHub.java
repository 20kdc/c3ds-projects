/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.userdata;

/**
 * Stuff between the user data cache and the hub.
 */
public interface IHubUserDataCacheBetweenCacheAndHub extends IHubUserDataCachePrivileged {

	/**
	 * Handles any business that needs to be handled on login.
	 * In particular, creates a reference held until logout.
	 * Returns false on failure.
	 */
	boolean hubLogin(INatsueUserData.Root root);

	/**
	 * Closes off the held reference created during login.
	 */
	void hubLogout(INatsueUserData.Root root);

	/**
	 * UDC system check
	 * @param detailed 
	 */
	void runSystemCheck(StringBuilder sb, boolean detailed);
}
