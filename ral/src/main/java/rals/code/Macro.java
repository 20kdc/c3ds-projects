/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.expr.*;
import rals.types.*;

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

	/**
	 * Inserts macro arguments into the context.
	 * Creates a copy list for stage 2.
	 * Returns null if nothing to copy.
	 */
	public static RALVAVar[] makeToCopy(MacroArg[] args, RALExpr[] a, ScopeContext macroContext) {
		boolean hadToCopyAnything = false;
		final RALVAVar[] toCopy = new RALVAVar[args.length];
		for (int i = 0; i < args.length; i++) {
			// Check this early
			RALExpr argExpr = a[i];
			argExpr.assertOutTypeSingleImpcast(args[i].type);
			// Now actually apply
			if (args[i].isInline) {
				// Note that we don't do a two-way check.
				// Instead any type errors caused by writing to the variable are handled during resolution or writeout.
				macroContext.scopedVariables.put(args[i].name, a[i]);
			} else {
				toCopy[i] = macroContext.newLocal(args[i].name, args[i].type);
				hadToCopyAnything = true;
			}
		}
		if (!hadToCopyAnything)
			return null;
		return toCopy;
	}
	/**
	 * Allocates VAs for and copies arguments into the compile context.
	 */
	public static void copyArgs(CodeWriter writer, CompileContext sc, RALVAVar[] toCopy, RALExpr[] a, String name, MacroArg[] args) {
		if (toCopy == null) {
			writer.writeComment("copyArgs given empty copy list, are you missing out on optimization?");
			return;
		}
		for (int i = 0; i < toCopy.length; i++) {
			if (toCopy[i] != null) {
				sc.allocVA(toCopy[i].handle);
				writer.writeComment(toCopy[i].getInlineCAOS(sc, false) + ": " + name + " arg " + i + ": " + args[i].type + " " + args[i].name);
				a[i].outCompile(writer, new RALExpr[] {toCopy[i]}, sc);
			}
		}
	}

	@Override
	public RALExpr instance(final RALExpr[] a, ScopeContext sc) {
		if (a.length != args.length)
			throw new RuntimeException("Macro called with wrong arg count (decomposition failure?)");
		// Our vars are going to be used outside of this context, so we attach vars and such to the parent.
		
		ScopeContext macroContext = new ScopeContext(sc);

		final RALVAVar[] toCopy = makeToCopy(args, a, macroContext);
		final RALExpr innards = code.resolve(macroContext);
		if (toCopy == null)
			return innards;
		return new RALExpr() {
			@Override
			public void inCompile(CodeWriter writer, String input, RALType inputExactType, CompileContext context) {
				copyArgs(writer, context, toCopy, a, name, args);
				innards.inCompile(writer, input, inputExactType, context);
			}
			@Override
			public RALType inType() {
				return innards.inType();
			}
			@Override
			public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
				copyArgs(writer, context, toCopy, a, name, args);
				innards.outCompile(writer, out, context);
			}
			@Override
			public RALType[] outTypes() {
				return innards.outTypes();
			}

			@Override
			public String toString() {
				return "macro arg copier of " + name;
			}
		};
	}
}
