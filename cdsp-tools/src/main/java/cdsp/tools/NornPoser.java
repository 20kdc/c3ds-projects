/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import javax.swing.JFrame;

import cdsp.common.app.JNorn;
import cdsp.common.data.skeleton.LoadedSkeleton;

/**
 * NornPoser!
 */
@SuppressWarnings("serial")
public class NornPoser extends JFrame {

	public final JNorn jNorn = new JNorn();

	public NornPoser(LoadedSkeleton ls) {
		super("NornPoser");
		setSize(800, 600);
		setLocationByPlatform(true);
		add(jNorn);
		jNorn.setParameters(ls, new int[ls.def.length]);
	}
	
}
