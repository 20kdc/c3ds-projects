/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.expr.RALStringVar;
import rals.lex.SrcPos;
import rals.types.RALType;

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
		final Object[] parts2 = new Object[parts.length];
		for (int i = 0; i < parts2.length; i++) {
			Object o = parts[i];
			if (o instanceof String) {
				parts2[i] = o;
			} else if (o instanceof RALExprUR) {
				parts2[i] = ((RALExprUR) o).resolve(csc);
			} else {
				throw new RuntimeException("RALInlineStatement takes Strings and RALExprsURs.");
			}
		}
		return new RALStatement(lineNumber) {
			@Override
			protected void compileInner(CodeWriter writer, CompileContext scope) {
				// scope for all the temporary VAs we may make
				try (CompileContext scope2 = new CompileContext(scope)) {
					StringBuilder interiorWriter = new StringBuilder();
					for (Object o : parts2) {
						if (o instanceof String) {
							interiorWriter.append(o);
						} else if (o instanceof RALExpr) {
							RALExpr re = (RALExpr) o;
							String inlineRepr = re.getInlineCAOS(scope2, false);
							if (inlineRepr != null) {
								interiorWriter.append(inlineRepr);
							} else {
								RALType[] slots = re.outTypes();
								RALStringVar[] vars = new RALStringVar[slots.length];
								for (int i = 0; i < vars.length; i++)
									vars[i] = scope2.allocVA(slots[i]);
								// Note that this goes to writer (for setup), while interiorWriter is building the main thing.
								re.outCompile(writer, vars, scope2);
								for (int i = 0; i < vars.length; i++) {
									if (i != 0)
										interiorWriter.append(" ");
									interiorWriter.append(vars[i].code);
								}
							}
						} else {
							throw new RuntimeException("RALInlineStatement intern takes Strings and RALExprs.");
						}
					}
					writer.writeCode(interiorWriter.toString());
				}
			}
		};
	}
}
