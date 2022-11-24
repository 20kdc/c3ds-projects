/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.types.*;

/**
 * Underscore, or the discard expression.
 * This is injected in ScopeContext alongside other "semi-keywords".
 */
public class RALDiscard extends RALExprSlice implements RALExprUR {
	public final RALSlot anySlot;

	public RALDiscard(TypeSystem ts, int len) {
		this(new RALSlot(ts.gAny, RALSlot.Perm.W), len);
	}
	private RALDiscard(RALSlot a, int len) {
		super(len);
		anySlot = a;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		return this;
	}

	@Override
	protected RALExprSlice sliceInner(int base, int length) {
		return new RALDiscard(anySlot, length);
	}

	@Override
	protected RALSlot slotInner(int index) {
		return anySlot;
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		// We need to discard this safely, soooo
		try (CompileContext ccr = new CompileContext(context)) {
			RALVarString.Fixed rsv = ccr.allocVA(inputExactType);
			rsv.writeCompile(0, input, inputExactType, context);
		}
	}

	@Override
	protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
		return RALSpecialInline.Discard;
	}
}
