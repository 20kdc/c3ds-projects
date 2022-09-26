/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.userdata;

/**
 * Privileged APIs (i.e. ones that modify data) for IHubUserDataCache.
 */
public interface IHubUserDataCachePrivileged extends IHubUserDataCache {
	@Override
	INatsueUserData.LongTermPrivileged openUserDataByNicknameLT(String name);

	@Override
	INatsueUserData.LongTermPrivileged openUserDataByUINLT(long uin);

	/**
	 * Given a user's username and password, provides an INatsueUserData.LongTermPrivileged (successful login), or null.
	 * The username will be automatically folded.
	 * Note this will still return the value for frozen accounts.
	 */
	INatsueUserData.LongTermPrivileged usernameAndPasswordLookup(String username, String password, boolean allowedToRegister);
}
