/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.ScopeContext;
import rals.expr.RALConstant;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.expr.RALStringVar;
import rals.lex.SrcPos;
import rals.types.RALType;

/**
 * Inline statement, made up of a set of parts.
 * The idea is that this is used to introduce the more "specific" CAOS commands to the system.
 */
public class RALInlineStatement extends RALStatement {
	public final Object[] parts;
	public RALInlineStatement(SrcPos pos, Object[] p) {
		super(pos);
		parts = p;
	}

	@Override
	protected void compileInner(StringBuilder writer, ScopeContext scope) {
		try (ScopeContext iScope = new ScopeContext(scope)) {
			StringBuilder interiorWriter = new StringBuilder();
			for (Object o : parts) {
				if (o instanceof String) {
					interiorWriter.append(o);
				} else if (o instanceof RALExprUR) {
					RALExprUR reu = (RALExprUR) o;
					RALExpr re = reu.resolve(iScope);
					String inlineRepr = re.getInlineCAOS();
					if (inlineRepr != null) {
						interiorWriter.append(inlineRepr);
					} else {
						RALType[] slots = re.outTypes(iScope.script);
						RALStringVar[] vars = new RALStringVar[slots.length];
						for (int i = 0; i < vars.length; i++)
							vars[i] = iScope.allocLocal(slots[i]);
						// Note that this goes to writer (for setup), while interiorWriter is building the main thing.
						re.outCompile(writer, vars, iScope.script);
						for (int i = 0; i < vars.length; i++) {
							if (i != 0)
								interiorWriter.append(" ");
							interiorWriter.append(vars[i].code);
						}
					}
				} else {
					throw new RuntimeException("RALInlineStatement takes Strings and RALExprs.");
				}
			}
			writer.append(interiorWriter.toString());
		}
	}
}
