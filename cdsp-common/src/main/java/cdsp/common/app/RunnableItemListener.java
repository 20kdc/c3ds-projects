/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package cdsp.common.app;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Forwards an ItemListener slot to a Runnable.
 */
public class RunnableItemListener implements ItemListener {
	public Runnable target;

	public RunnableItemListener(Runnable r) {
		target = r;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getStateChange() == ItemEvent.SELECTED)
			target.run();
	}
}
