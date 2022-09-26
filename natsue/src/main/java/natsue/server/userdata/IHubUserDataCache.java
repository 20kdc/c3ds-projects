/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.userdata;

/**
 * Responsible for managing user data objects.
 */
public interface IHubUserDataCache {
	/**
	 * Gets user data by nickname.
	 * The nickname will be automatically folded.
	 * Can and will return null.
	 * There is no guarantee the data will not become stale over any period of time.
	 * However there is also no guarantee it will remain constant, either.
	 */
	INatsueUserData getUserDataByNickname(String name);

	/**
	 * Gets user data by their UIN.
	 * Can and will return null.
	 * There is no guarantee the data will not become stale over any period of time.
	 * However there is also no guarantee it will remain constant, either.
	 */
	INatsueUserData getUserDataByUIN(long uin);

	/**
	 * Gets user data by nickname.
	 * The nickname will be automatically folded.
	 * Can and will return null.
	 * A reference is added to the returned object - this reference is owned by the caller.
	 * The caller is expected to drop the reference using close(), at which point data may become stale.
	 */
	INatsueUserData.LongTerm openUserDataByNicknameLT(String name);

	/**
	 * Gets user data by their UIN.
	 * Can and will return null.
	 * A reference is added to the returned object - this reference is owned by the caller.
	 * The caller is expected to drop the reference using close(), at which point data may become stale.
	 */
	INatsueUserData.LongTerm openUserDataByUINLT(long uin);
}
