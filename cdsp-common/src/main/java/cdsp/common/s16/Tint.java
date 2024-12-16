/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

/**
 * Tint data.
 */
public final class Tint {
	public final int r, g, b, rot, swap;

	public Tint() {
		this.r = this.g = this.b = this.rot = this.swap = 128;
	}

	public Tint(int r, int g, int b, int rot, int swap) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.rot = rot;
		this.swap = swap;
	}

	public Tint withR(int r) {
		return new Tint(r, g, b, rot, swap);
	}

	public Tint withG(int g) {
		return new Tint(r, g, b, rot, swap);
	}

	public Tint withB(int b) {
		return new Tint(r, g, b, rot, swap);
	}

	public Tint withRot(int rot) {
		return new Tint(r, g, b, rot, swap);
	}

	public Tint withSwap(int swap) {
		return new Tint(r, g, b, rot, swap);
	}

	public boolean isIdentity() {
		return r == 128 && g == 128 && b == 128 && rot == 128 && swap == 128;
	}
}
