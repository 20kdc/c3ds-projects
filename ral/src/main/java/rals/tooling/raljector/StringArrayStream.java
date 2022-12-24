/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

/**
 * Eleventh hour restructuring be like:
 */
public class StringArrayStream {
	public String[] contents;
	public int index;
	public String help = "Unexpected lack of parameters";
	public StringArrayStream(String[] c) {
		contents = c;
	}
	public String get() {
		if (index >= contents.length)
			throw new RuntimeException(help);
		return contents[index++];
	}
	public void expectNoMore() {
		if (index != contents.length)
			throw new RuntimeException(help);
	}
	public boolean hasMore() {
		return index < contents.length;
	}
	public int remaining() {
		return contents.length - index;
	}
}
