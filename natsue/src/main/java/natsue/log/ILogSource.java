/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.log;

/**
 * Convenience class for logging.
 * TODO: REWRITE THIS BIT, THE STACK OVERFLOW BUG DURING TEST1 WAS HILARIOUS AND BLOODY STUPID
 */
public interface ILogSource extends ILogProvider {
	ILogProvider getLogParent();

	default void log(String text) {
		getLogParent().log(toString(), text);
	}
	default void log(Throwable ex) {
		getLogParent().log(toString(), ex);
	}

	@Override
	default void log(String source, String text) {
		getLogParent().log(toString() + ": " + source, text);
	}
	@Override
	default void log(String source, Throwable ex) {
		getLogParent().log(toString() + ": " + source, ex);
	}
}
