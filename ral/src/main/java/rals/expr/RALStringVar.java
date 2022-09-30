/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * For trivial expressions & variables.
 * Goes nicely with inline statements.
 */
public class RALStringVar implements RALExpr {
	public final String code;
	public final RALType type;
	public final boolean isWritable;
	public RALStringVar(String c, RALType ot, boolean w) {
		code = c;
		type = ot;
		isWritable = w;
	}

	@Override
	public RALType inType(ScriptContext context) {
		return type;
	}

	@Override
	public RALType[] outTypes(ScriptContext context) {
		return new RALType[] {
			type
		};
	}

	@Override
	public void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context) {
		if (!isWritable)
			throw new RuntimeException("Not writable");
		switch (inputExactType.majorType) {
		case Agent:
			writer.append("seta ");
			break;
		case String:
			writer.append("sets ");
			break;
		case Value:
			writer.append("setv ");
			break;
		default:
			throw new RuntimeException("Unknown major type of " + input);
		}
		writer.append(code);
		writer.append(" ");
		writer.append(input);
		writer.append("\n");
	}

	@Override
	public void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context) {
		if (!isWritable)
			throw new RuntimeException("Not writable");
		out[0].inCompile(writer, code, type, context);
	}
}
