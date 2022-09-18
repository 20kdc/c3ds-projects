/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database;

/**
 * Abstract interface for authentication tasks.
 */
public interface IAuthProvider {
	/**
	 * Gets the name of a user by their UIN.
	 * Can and will return null.
	 */
	public String getNameByUIN(long uin);

	/**
	 * Given a user's username and password, provides a UIN, or zero.
	 */
	public long usernameAndPasswordToUIN(String username, String password);

	/**
	 * Gets a UIN reserved for this server.
	 */
	public long getServerUIN();
}
