/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.userdata;

/**
 * Proxy for privileged user data APIs.
 */
public interface IHubUserDataCachePrivilegedProxy extends IHubUserDataCachePrivileged {
	/**
	 * Returns the underlying user data cache instance used by an IHubUserCache proxy.
	 */
	IHubUserDataCachePrivileged getUserDataCachePrivileged();

	@Override
	default INatsueUserData getUserDataByNickname(String name) {
		return getUserDataCachePrivileged().getUserDataByNickname(name);
	}

	@Override
	default INatsueUserData getUserDataByUIN(long uin) {
		return getUserDataCachePrivileged().getUserDataByUIN(uin);
	}

	@Override
	default INatsueUserData.LongTermPrivileged openUserDataByNicknameLT(String name) {
		return getUserDataCachePrivileged().openUserDataByNicknameLT(name);
	}

	@Override
	default INatsueUserData.LongTermPrivileged openUserDataByUINLT(long uin) {
		return getUserDataCachePrivileged().openUserDataByUINLT(uin);
	}

	@Override
	default INatsueUserData.LongTermPrivileged usernameAndPasswordLookup(String username, String password, boolean allowedToRegister) {
		return getUserDataCachePrivileged().usernameAndPasswordLookup(username, password, allowedToRegister);
	}

}
