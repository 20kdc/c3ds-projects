/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Don't even ask
 */
public final class Signal<T> {
	public HashSet<Consumer<T>> listeners = new HashSet<>();
	public void add(Consumer<T> ctl) {
		listeners.add(ctl);
	}
	public void remove(Consumer<Object> refreshEv) {
		listeners.remove(refreshEv);
	}
	public void fire(T o) {
		for (Consumer<T> ct : listeners)
			ct.accept(o);
	}
}
