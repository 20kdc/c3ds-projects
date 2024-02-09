/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.cctx.*;
import rals.code.*;
import rals.types.*;

/**
 * For trivial expressions and variables.
 * Goes nicely with inline statements.
 */
public class RALVarString extends RALVarBase {
	public RALVarString(RALType ot, boolean w) {
		super(ot, w);
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		out.writeCompile(0, getInlineCAOS(0, false, context), type.majorType, context);
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType.Major inputExactType, CompileContext context) {
		if (!isWritable)
			throw new RuntimeException("Var " + this + " is not writable");
		// Note that if we don't *have* the input's exact type, we can still try our OWN type.
		// This particularly applies to casts on in-place reads.
		writeSet(context.writer, getInlineCAOS(index, true, context), input, inputExactType.autoPromote(type.majorType));
	}

	/**
	 * Writes a seta/sets/setv. WARNING: ALL CALLS TO THIS SHOULD BE DOING AUTOPROMOTING.
	 * That is, if you have a "local" type, you should use inputExactType.autoPromote(that local major type)
	 * inputExactType still overrules, but this catches edge cases.
	 */
	public static void writeSet(CodeWriter writer, String code, String input, RALType.Major inputExactType) {
		String set;
		// [CAOS]
		switch (inputExactType) {
		case Agent:
			set = "seta ";
			break;
		case String:
			set = "sets ";
			break;
		case Value:
			set = "setv ";
			break;
		case ByteString:
			throw new RuntimeException("Major type of " + input + " (" + inputExactType + ") is ByteString, which can't be stored.");
		default:
			throw new RuntimeException("Unknown major type of " + input + " (" + inputExactType + ") - you will need to cast this value");
		}
		// Peephole optimization: if code and input are exactly equal, skip the set.
		// There is no conceivable reason the compiler would want to do a dummy set like this.
		// If a user wants to use seta/sets/setv as an assertion they can use an inline statement.
		if (code.equals(input))
			return;
		writer.writeCode(set + code + " " + input);
	}

	/**
	 * Variable with fixed code.
	 * DO NOT use this for VAs! It breaks optimizations.
	 */
	public static class Fixed extends RALVarString implements RALExprUR {
		public final String code;
		public Fixed(String c, RALType rt, boolean w) {
			super(rt, w);
			code = c;
		}

		@Override
		public String toString() {
			return (isWritable ? "SVW" : "SV") + "[" + code + "!" + type + "]";
		}

		@Override
		public String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			if (write && !isWritable)
				return null;
			return code;
		}

		@Override
		public RALExprSlice resolveInner(ScopeContext scope) {
			return this;
		}
	}
}
