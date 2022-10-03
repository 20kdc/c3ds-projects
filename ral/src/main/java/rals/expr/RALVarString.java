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
 * For trivial expressions & variables.
 * Goes nicely with inline statements.
 */
public class RALVarString extends RALVarBase {
	public RALVarString(RALType ot, boolean w) {
		super(ot, w);
	}

	@Override
	protected void readCompileInner(RALExprSlice out, CompileContext context) {
		out.writeCompile(0, getInlineCAOS(0, false, context), type, context);
	}

	@Override
	protected void writeCompileInner(int index, String input, RALType inputExactType, CompileContext context) {
		if (!isWritable)
			throw new RuntimeException("Var " + this + " is not writable");
		writeSet(context.writer, getInlineCAOS(index, true, context), input, inputExactType);
	}

	public static void writeSet(CodeWriter writer, String code, String input, RALType inputExactType) {
		String set;
		switch (inputExactType.majorType) {
		case Agent:
			set = "seta ";
			break;
		case String:
			set = "sets ";
			break;
		case Value:
			set = "setv ";
			break;
		default:
			throw new RuntimeException("Unknown major type of " + input + " (" + inputExactType + ")");
		}
		writer.writeCode(set + code + " " + input);
	}

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
		public String getInlineCAOSInner(int index, boolean write, CompileContext context) {
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
