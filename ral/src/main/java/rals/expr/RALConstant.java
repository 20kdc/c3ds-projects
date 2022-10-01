/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;


import rals.code.CompileContext;
import rals.code.ScopeContext;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * A constant.
 * The toString returns these in CAOS form.
 */
public abstract class RALConstant implements RALExpr, RALExprUR {
	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		return this;
	}

	@Override
	public String getInlineCAOS(CompileContext context) {
		return toString();
	}

	@Override
	public String toString() {
		throw new RuntimeException("Not implemented, should be!");
	}

	public static abstract class Single extends RALConstant {
		public final RALType type;
		public Single(RALType r) {
			type = r;
		}

		public abstract RALConstant.Single cast(RALType rt);

		@Override
		public void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context) {
			out[0].inCompile(writer, toString(), type, context);
		}

		@Override
		public RALType[] outTypes() {
			return new RALType[] {type};
		}

		@Override
		public RALType inType() {
			throw new RuntimeException("Constants are not writable");
		}

		@Override
		public void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context) {
			throw new RuntimeException("Constants are not writable");
		}

		@Override
		public RALExpr resolve(ScopeContext context) {
			return this;
		}
	}

	public static class Str extends Single {
		public final String value;
		public Str(TypeSystem ts, String s) {
			super(ts.gString);
			value = s;
		}
		public Str(RALType t, String s) {
			super(t);
			value = s;
		}

		@Override
		public Str cast(RALType rt) {
			return new Str(rt, value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Str)
				return value.equals(((Str) obj).value);
			return false;
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

	public static class Int extends Single {
		public final int value;
		public Int(TypeSystem ts, int v) {
			super(ts.gInteger);
			value = v;
		}
		public Int(RALType t, int v) {
			super(t);
			value = v;
		}

		@Override
		public Int cast(RALType rt) {
			return new Int(rt, value);
		}

		@Override
		public int hashCode() {
			return value;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Int) {
				return value == ((Int) obj).value;
			} else if (obj instanceof Flo) {
				return ((double) value) == ((Flo) obj).value;
			}
			return false;
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	public static class Flo extends Single {
		public final float value;
		public Flo(TypeSystem ts, float f) {
			super(ts.gFloat);
			value = f;
		}
		public Flo(RALType t, float f) {
			super(t);
			value = f;
		}

		@Override
		public Flo cast(RALType rt) {
			return new Flo(rt, value);
		}

		@Override
		public int hashCode() {
			return Float.hashCode(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Int) {
				return value == (double) ((Int) obj).value;
			} else if (obj instanceof Flo) {
				return value == ((Flo) obj).value;
			}
			return false;
		}

		@Override
		public String toString() {
			return Float.toString(value);
		}
	}
}
