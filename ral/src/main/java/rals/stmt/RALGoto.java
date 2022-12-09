/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.cctx.*;
import rals.code.ScopeContext;
import rals.diag.SrcRange;

/**
 * Advanced goto for an advanced era.
 */
public class RALGoto extends RALStatementUR {
	public final ILabelHandle globalHandle;
	public RALGoto(SrcRange sp, ILabelHandle g) {
		super(sp);
		globalHandle = g;
	}

	@Override
	public String toString() {
		return "goto " + globalHandle + ";";
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		return new RALStatement(extent) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext context) {
				context.labelScope.compileJump(globalHandle, context);
			}
			@Override
			public String toString() {
				return "goto " + globalHandle + ";";
			}
		};
	}

}
