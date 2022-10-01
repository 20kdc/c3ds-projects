/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * For trivial expressions & variables.
 * Goes nicely with inline statements.
 */
public class RALStringVar implements RALExpr, RALExprUR {
	public final String code;
	public final RALType type;
	public final boolean isWritable;
	public RALStringVar(String c, RALType ot, boolean w) {
		code = c;
		type = ot;
		isWritable = w;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		return this;
	}

	@Override
	public String toString() {
		return (isWritable ? "SVW" : "SV") + "[" + code + "!" + type + "]";
	}

	@Override
	public RALType inType() {
		return type;
	}

	@Override
	public RALType[] outTypes() {
		return new RALType[] {
			type
		};
	}

	@Override
	public void inCompile(CodeWriter writer, String input, RALType inputExactType, CompileContext context) {
		if (!isWritable)
			throw new RuntimeException("Not writable");
		writeSet(writer, code, input, inputExactType);
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

	@Override
	public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
		out[0].inCompile(writer, code, type, context);
	}

	@Override
	public String getInlineCAOS(CompileContext context, boolean write) {
		if (write && !isWritable)
			return null;
		return code;
	}
}
