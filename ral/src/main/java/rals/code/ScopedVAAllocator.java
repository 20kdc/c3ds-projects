/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.LinkedList;

/**
 * This is usable by RALStatements for their various internal purposes.
 */
public class ScopedVAAllocator implements IVAAllocator, AutoCloseable {
	public final IVAAllocator parent;
	public final LinkedList<Integer> freeList = new LinkedList<>();
	public final LinkedList<Integer> allList = new LinkedList<>();
	/**
	 * If this is set to true, we deliberately leak variables upwards.
	 * This is presumably on the basis that they're going to be needed later in the parent context.
	 */
	public boolean leak;

	public ScopedVAAllocator(IVAAllocator p) {
		parent = p;
	}

	/**
	 * Ensures the given amount of free VA slots exist.
	 * Used for the root allocator.
	 * This is particularly important now that fixed VA slots can be allocated,
	 *  as the linear allocator doesn't actually do fixed slots.
	 */
	public void ensureFree(int amount) {
		while (freeList.size() < amount) {
			int va = parent.allocVA();
			allList.add(va);
			freeList.add(va);
		}
	}

	@Override
	public int allocVA() {
		if (freeList.size() > 0)
			return freeList.remove();
		int va = parent.allocVA();
		allList.add(va);
		return va;
	}

	@Override
	public void allocVA(int i) {
		if (allList.contains(i)) {
			// it's already locally managed
			if (!freeList.contains(i))
				throw new RuntimeException("VA " + i + " already in use");
		} else {
			// pull from parent
			parent.allocVA(i);
			allList.add(i);
		}
	}

	@Override
	public void releaseVA(int i) {
		freeList.add(i);
	}

	public void close() {
		if (leak)
			return;
		for (Integer it : allList)
			parent.releaseVA(it);
	}
}
