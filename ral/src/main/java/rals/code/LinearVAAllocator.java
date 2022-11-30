/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

/**
 * Useful as part of the machinery that creates the root VA allocator.
 */
public class LinearVAAllocator implements IVAAllocator {
	public int currentVA = 0;

	@Override
	public int allocVA() {
		int va = currentVA++;
		if (va >= 100)
			throw new RuntimeException("Out of VAs");
		return va;
	}

	@Override
	public void allocVA(int i) {
		throw new RuntimeException("Cannot perform fixed VA allocation from linear allocator");
	}

	@Override
	public void releaseVA(int i) {
	}
}
