/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.cctx.IEHHandle;
import rals.expr.RALSlot;
import rals.types.RALType;

/**
 * Argument/parameter to a macro.
 */
public final class MacroArg implements IEHHandle {
	public final RALType type;
	public final RALSlot.Perm isInline;
	public final String name;
	public MacroArg(RALType rt, RALSlot.Perm in, String n) {
		type = rt;
		name = n;
		isInline = in;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MacroArg) {
			MacroArg other = (MacroArg) obj;
			return type.equals(other.type) && isInline.equals(other.isInline) && name.equals(other.name);
		}
		return false;
	}

	@Override
	public String toString() {
		return "macro arg " + name;
	}

	public RALSlot.Perm computeRequiredPerms() {
		if (isInline != null)
			return isInline;
		return RALSlot.Perm.R;
	}
}
