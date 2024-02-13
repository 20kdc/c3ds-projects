/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Standard output.
 */
public final class StdoutLogProvider implements ILogProvider {
	@Override
	public void log(ILogSource source, String text) {
		while (source != null) {
			text = source + ": " + text;
			ILogProvider parent = source.getLogParent();
			if (parent instanceof ILogSource) {
				source = (ILogSource) parent;
			} else {
				source = null;
			}
		}
		synchronized (this) {
			System.out.println(Instant.now().toString() + ": " + text);
		}
	}

	@Override
	public void log(ILogSource source, Throwable ex) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		log(source, sw.toString());
	}
}