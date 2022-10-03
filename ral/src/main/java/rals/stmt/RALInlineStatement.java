/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.expr.*;
import rals.lex.*;
import rals.types.*;

/**
 * Inline statement, made up of a set of parts.
 * The idea is that this is used to introduce the more "specific" CAOS commands to the system.
 */
public class RALInlineStatement extends RALStatementUR {
	public final Object[] parts;
	public RALInlineStatement(SrcPos pos, Object[] p) {
		super(pos);
		parts = p;
	}

	@Override
	public RALStatement resolveInner(ScopeContext csc) {
		return new Resolved(lineNumber, resolveParts(parts, csc));
	}

	public static final class Resolved extends RALStatement {
		private final Object[] parts2;

		public Resolved(SrcPos ln, Object[] parts2) {
			super(ln);
			this.parts2 = parts2;
		}

		@Override
		public String toString() {
			return "inline (...);";
		}

		@Override
		protected void compileInner(CodeWriter writer, CompileContext scope) {
			// scope for all the temporary VAs we may make
			try (CompileContext scope2 = new CompileContext(scope)) {
				scope.writer.writeCode(compileResolvedParts(parts2, scope2, false));
			}
		}
	}

	public static Object[] resolveParts(Object[] parts, ScopeContext csc) {
		final Object[] parts2 = new Object[parts.length];
		for (int i = 0; i < parts2.length; i++) {
			Object o = parts[i];
			if (o instanceof String) {
				parts2[i] = o;
			} else if (o instanceof RALExprUR) {
				parts2[i] = ((RALExprUR) o).resolve(csc);
			} else {
				throw new RuntimeException("Inline sections take Strings and RALExprsURs.");
			}
		}
		return parts2;
	}

	public static String compileResolvedParts(Object[] resolvedParts, CompileContextNW scope) {
		return compileResolvedParts(resolvedParts, (CompileContext) scope, true);
	}

	public static String compileResolvedParts(Object[] resolvedParts, CompileContext scope) {
		return compileResolvedParts(resolvedParts, scope, false);
	}

	private static String compileResolvedParts(Object[] resolvedParts, CompileContext scope, boolean inlineOnly) {
		StringBuilder interiorWriter = new StringBuilder();
		for (Object o : resolvedParts) {
			if (o instanceof String) {
				interiorWriter.append(o);
			} else if (o instanceof RALExprSlice) {
				RALExprSlice re = (RALExprSlice) o;
				if (!inlineOnly) {
					// Automatically cache vars that we need to.
					// Note that if inlineOnly is set, we're not allowed to do this.
					boolean[] inline = new boolean[re.length];
					for (int i = 0; i < re.length; i++)
						inline[i] = re.getInlineCAOS(i, false, scope) != null;
					VarCacher vc = new VarCacher(re, inline, null);
					vc.writeCacheCode(scope);
					re = vc.finishedOutput;
				}
				for (int i = 0; i < re.length; i++) {
					String inlineRepr = re.getInlineCAOS(i, false, scope);
					if (inlineRepr == null)
						return null;
					if (i != 0)
						interiorWriter.append(" ");
					interiorWriter.append(inlineRepr);
				}
			} else {
				throw new RuntimeException("RALInlineStatement intern takes Strings and RALExprs.");
			}
		}
		return interiorWriter.toString();
	}
}
