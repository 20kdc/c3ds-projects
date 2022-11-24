/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.types.RALType;

/**
 * The compile-time details on a slot. 
 */
public class RALSlot {
	public final RALType type;
	public final Perm perms;

	public RALSlot(RALType t, Perm p) {
		type = t;
		perms = p;
	}

	@Override
	public String toString() {
		return type + "/" + perms;
	}

	/**
	 * Permissions for slots.
	 */
	public enum Perm {
		None(false, false),
		R(true, false),
		W(false, true),
		RW(true, true);
	
		public final boolean read, write;
	
		Perm(boolean r, boolean w) {
			read = r;
			write = w;
		}

		public Perm denyWrite() {
			if (!write)
				return this;
			if (this == W)
				return None;
			if (this == RW)
				return R;
			throw new RuntimeException("Bad deny write");
		}

		public Perm denyRead() {
			if (!read)
				return this;
			if (this == R)
				return None;
			if (this == RW)
				return W;
			throw new RuntimeException("Bad deny read");
		}
	}
}
