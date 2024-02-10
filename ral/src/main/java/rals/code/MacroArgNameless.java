/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.expr.RALSlot;
import rals.types.RALType;

/**
 * Argument/parameter to a macro.
 */
public class MacroArgNameless {
	public final RALType type;
	public final RALSlot.Perm isInline;
	public MacroArgNameless(RALType rt, RALSlot.Perm in) {
		type = rt;
		isInline = in;
	}

	@Override
	public String toString() {
		if (isInline != null) {
			if (isInline == RALSlot.Perm.R)
				return type.getFullDescription() + " @";
			if (isInline == RALSlot.Perm.RW)
				return type.getFullDescription() + " @=";
			return type.getFullDescription() + " @?";
		}
		return type.getFullDescription();
	}

	public RALSlot.Perm computeRequiredPerms() {
		if (isInline != null)
			return isInline;
		return RALSlot.Perm.R;
	}

	public boolean canBeCastTo(MacroArgNameless other) {
		// for now be super restrictive about this
		if (!other.type.canImplicitlyCast(type))
			return false;
		if (!type.canImplicitlyCast(other.type))
			return false;
		if (isInline != other.isInline)
			return false;
		return true;
	}
}
