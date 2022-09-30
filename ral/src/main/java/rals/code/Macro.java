/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.io.StringWriter;

import rals.expr.RALCallable;
import rals.expr.RALCast;
import rals.expr.RALExpr;
import rals.expr.RALExprUR;
import rals.expr.RALStringVar;
import rals.types.RALType;

/**
 * Macros are used to replicate functions because CAOS doesn't have a proper version of those.
 */
public class Macro implements RALCallable {
	public final String name;
	public final MacroArg[] args;
	public final RALExprUR code;

	public Macro(String n, MacroArg[] a, RALExprUR c) {
		name = n;
		args = a;
		code = c;
	}

	@Override
	public RALExpr instance(final RALExpr[] a, ScopeContext sc) {
		if (a.length != args.length)
			throw new RuntimeException("Macro called with wrong arg count (decomposition failure?)");
		try (ScopeContext macroContext = new ScopeContext(sc)) {
			final RALStringVar[] toCopy = new RALStringVar[args.length];
			for (int i = 0; i < a.length; i++) {
				// Check this early
				RALExpr argExpr = a[i];
				RALType[] rt = argExpr.outTypes(sc.script);
				if (rt.length != 1)
					throw new RuntimeException("Macro arg " + i + ":" + args[i].name + " given >1 value.");
				rt[0].implicitlyCastOrThrow(args[i].type, rt[0], args[i]);
				// Now actually apply
				if (args[i].isInline) {
					// Note that we don't do a two-way check.
					// Instead any type errors caused by writing to the variable are handled during resolution or writeout.
					macroContext.scopedVariables.put(args[i].name, a[i]);
				} else {
					toCopy[i] = macroContext.allocLocal(args[i].name, args[i].type);
				}
			}
			final RALExpr innards = code.resolve(macroContext);
			return new RALExpr() {
				private void copyArgs(StringBuilder writer, ScriptContext sc) {
					for (int i = 0; i < toCopy.length; i++) {
						if (toCopy[i] != null) {
							writer.append(" * ");
							writer.append(toCopy[i].code);
							writer.append(": " + name + " arg ");
							writer.append(i);
							writer.append(": ");
							writer.append(args[i].type);
							writer.append(" ");
							writer.append(args[i].name);
							writer.append("\n");
							a[i].outCompile(writer, new RALExpr[] {toCopy[i]}, sc);
						}
					}
				}
				@Override
				public void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context) {
					copyArgs(writer, context);
					innards.inCompile(writer, input, inputExactType, context);
				}
				@Override
				public RALType inType(ScriptContext context) {
					return innards.inType(context);
				}
				@Override
				public void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context) {
					copyArgs(writer, context);
					innards.outCompile(writer, out, context);
				}
				@Override
				public RALType[] outTypes(ScriptContext context) {
					return innards.outTypes(context);
				}
			};
		}
	}
}
