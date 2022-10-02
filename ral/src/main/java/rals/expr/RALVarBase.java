/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.types.RALType;

/**
 * Please do not instanceof, for the love of kittens
 */
public abstract class RALVarBase extends RALExprSlice {
	public final RALType type;
	public final boolean isWritable;

	public RALVarBase(RALType t, boolean w) {
		super(1);
		type = t;
		isWritable = w;
	}

	@Override
	protected RALType readTypeInner(int index) {
		return type;
	}

	@Override
	protected RALType writeTypeInner(int index) {
		if (!isWritable)
			throw new RuntimeException("Var " + this + " not writable");
		return type;
	}
}
