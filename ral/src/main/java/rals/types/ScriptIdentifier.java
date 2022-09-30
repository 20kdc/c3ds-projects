/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

/**
 * A script identifier, or if you really like, a message identifier.
 */
public final class ScriptIdentifier {
	public final Classifier classifier;
	public final int script;

	public ScriptIdentifier(Classifier c, int s) {
		classifier = c;
		script = s;
	}

	@Override
	public int hashCode() {
		return script ^ classifier.hashCode();
	}

	@Override
	public boolean equals(Object var1) {
		if (var1 instanceof ScriptIdentifier) {
			if (!((ScriptIdentifier) var1).classifier.equals(classifier))
				return false;
			if (((ScriptIdentifier) var1).script != script)
				return false;
			return true;
		}
		return false;
	}
}
