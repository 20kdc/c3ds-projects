/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.log;

/**
 * Convenience class for logging.
 */
public interface ILogSource extends ILogProvider {
	ILogProvider getLogParent();

	@Override
	default void log(ILogSource source, String text) {
		getLogParent().log(source, text);
	}
	@Override
	default void log(ILogSource source, Throwable ex) {
		getLogParent().log(source, ex);
	}

	default void log(String text) {
		log(this, text);
	}
	default void log(Throwable ex) {
		log(this, ex);
	}
}
