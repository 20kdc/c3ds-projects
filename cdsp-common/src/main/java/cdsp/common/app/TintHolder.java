/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

/**
 * A thing with inherent tint.
 */
public interface TintHolder {
	public int getTintR();
	public int getTintG();
	public int getTintB();
	public int getTintRot();
	public int getTintSwap();
	public void setTint(int r, int g, int b, int rot, int swap);

	public default void copyFrom(TintHolder other) {
		setTint(other.getTintR(), other.getTintG(), other.getTintB(), other.getTintRot(), other.getTintSwap());
	}
}
