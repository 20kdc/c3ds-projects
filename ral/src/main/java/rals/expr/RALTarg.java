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
import rals.types.RALType;

/**
 * INTENSIFYING SIGHING
 */
public class RALTarg implements RALExpr, RALExprUR {
	public final RALType type;
	public RALTarg(RALType ot) {
		type = ot;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		return this;
	}

	@Override
	public String toString() {
		return "targ";
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
		writer.writeCode("targ " + input);
	}

	@Override
	public void outCompile(CodeWriter writer, RALExpr[] out, CompileContext context) {
		out[0].inCompile(writer, "targ", type, context);
	}

	@Override
	public SpecialInline getSpecialInline(CompileContext context) {
		return SpecialInline.Targ;
	}
}
