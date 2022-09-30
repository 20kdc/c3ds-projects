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
import rals.types.TypeSystem;

/**
 * A constant.
 * The toString returns these in CAOS form.
 */
public class RALConstant implements RALExpr {
	public final RALType type;
	public RALConstant(RALType r) {
		type = r;
	}

	@Override
	public void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context) {
		out[0].inCompile(writer, toString(), type, context);
	}

	@Override
	public RALType[] outTypes(ScriptContext context) {
		return new RALType[] {type};
	}

	@Override
	public RALType inType(ScriptContext context) {
		throw new RuntimeException("Constants are not writable");
	}

	@Override
	public void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context) {
		throw new RuntimeException("Constants are not writable");
	}

	public static class Str extends RALConstant {
		public final String value;
		public Str(TypeSystem ts, String s) {
			super(ts.gString);
			value = s;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append('\"');
			for (char c : value.toCharArray()) {
				if ((c == '\\') || (c == '\"')) {
					sb.append('\\');
					sb.append(c);
				} else if (c == '\r') {
					sb.append("\\r");
				} else if (c == '\n') {
					sb.append("\\n");
				} else if (c == '\t') {
					sb.append("\\t");
				} else if (c == 0) {
					sb.append("\\0");
				} else {
					sb.append(c);
				}
			}
			sb.append('\"');
			return sb.toString();
		}
	}

	public static class Int extends RALConstant {
		public final int value;
		public Int(TypeSystem ts, int v) {
			super(ts.gInteger);
			value = v;
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	public static class Flo extends RALConstant {
		public final float value;
		public Flo(TypeSystem ts, float f) {
			super(ts.gFloat);
			value = f;
		}

		@Override
		public String toString() {
			return Float.toString(value);
		}
	}
}
