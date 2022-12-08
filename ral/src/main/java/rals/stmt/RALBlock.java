/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import java.util.LinkedList;

import rals.cctx.*;
import rals.code.ScopeContext;
import rals.diag.*;

/**
 * Block "statement"
 */
public class RALBlock extends RALStatementUR {
	public LinkedList<RALStatementUR> content = new LinkedList<>();
	public final boolean isScopeBreaking;
	public RALBlock(SrcPos lineNumber, boolean scopeBreaking) {
		super(lineNumber);
		isScopeBreaking = scopeBreaking;
	}

	@Override
	public RALStatement resolveInner(ScopeContext scope) {
		if (isScopeBreaking)
			scope = new ScopeContext(scope);

		final LinkedList<RALStatement> content2 = new LinkedList<>();
		for (RALStatementUR ur : content)
			content2.add(ur.resolve(scope));

		return new Resolved(extent, content2, isScopeBreaking);
	}

	public static final class Resolved extends RALStatement {
		private final LinkedList<RALStatement> content2;
		private final boolean isScopeBreaking;

		public Resolved(SrcRange ln, LinkedList<RALStatement> content2, boolean sb) {
			super(ln);
			this.content2 = content2;
			isScopeBreaking = sb;
		}

		@Override
		protected void compileInner(CodeWriter writer, CompileContext cc) {
			if (isScopeBreaking) {
				try (CompileContext innerScope = new CompileContext(cc)) {
					for (RALStatement rl : content2)
						rl.compile(writer, innerScope);
				}
			} else {
				for (RALStatement rl : content2)
					rl.compile(writer, cc);
			}
		}

		@Override
		public String toString() {
			return "{...}";
		}
	}
}
