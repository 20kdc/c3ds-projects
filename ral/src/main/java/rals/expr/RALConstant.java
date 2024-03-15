/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.Set;

import cdsp.common.data.bytestring.ByteString;
import rals.caos.CAOSUtils;
import rals.cctx.*;
import rals.code.*;
import rals.types.*;

/**
 * A constant.
 * The toString returns these in CAOS form.
 */
public abstract class RALConstant extends RALExprSlice implements RALExprUR {
	public RALConstant(int len) {
		super(len);
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts, Set<String> scopedVariables) {
		return this;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		return this;
	}

	/**
	 * BIG SCARY WARNING:
	 * Right now the difference between single constants and multiple constants is NOT checked.
	 * Given the compiler's only just escaped a feature freeze because ANIM is kind of important,
	 * this probably isn't going to change anytime soon.
	 */
	public static abstract class Single extends RALConstant {
		public final RALType type;
		public final RALSlot slot;
		public Single(RALType r) {
			super(1);
			type = r;
			slot = new RALSlot(r, RALSlot.Perm.R);
		}

		@Override
		public String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			if (write)
				return null;
			return toString();
		}

		public abstract ByteString getInlineCAOSConst(CompileContextNW context);

		public abstract RALConstant.Single cast(RALType rt);

		@Override
		protected RALSlot slotInner(int index) {
			return slot;
		}

		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			out.writeCompile(0, toString(), type.majorType, context);
		}
	}

	public static class Str extends Single {
		/**
		 * Value. It is very important to keep in mind CAOSUtils.CAOS_CHARSET.
		 */
		public final String valueChars;
		/**
		 * Value as bytes.
		 */
		public final ByteString valueBytes;

		public Str(TypeSystem ts, String s) {
			super(ts.gString);
			valueChars = s;
			valueBytes = new ByteString(s, CAOSUtils.CAOS_CHARSET);
		}

		public Str(RALType t, String s) {
			super(t);
			valueChars = s;
			valueBytes = new ByteString(s, CAOSUtils.CAOS_CHARSET);
		}

		public Str(TypeSystem ts, ByteString s) {
			super(ts.gString);
			valueChars = s.toString(CAOSUtils.CAOS_CHARSET);
			valueBytes = s;
		}

		public Str(RALType t, ByteString s) {
			super(t);
			valueChars = s.toString(CAOSUtils.CAOS_CHARSET);
			valueBytes = s;
		}

		@Override
		public ByteString getInlineCAOSConst(CompileContextNW context) {
			return CAOSUtils.stringIntoCAOSConstant(valueBytes, context.typeSystem.codeGenFeatureLevel).toByteString();
		}

		@Override
		public Str cast(RALType rt) {
			return new Str(rt, valueBytes);
		}

		@Override
		public int hashCode() {
			return valueChars.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Str)
				return valueChars.equals(((Str) obj).valueChars);
			return false;
		}

		@Override
		public String toString() {
			return CAOSUtils.stringIntoCAOSConstant(valueBytes, CodeGenFeatureLevel.customEngine).toString(CAOSUtils.CAOS_CHARSET);
		}
	}

	public static abstract class Number extends Single {
		public Number(RALType r) {
			super(r);
		}

		public abstract float toFloat();
	}

	public static class Int extends Number {
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
		public float toFloat() {
			return value;
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
		public ByteString getInlineCAOSConst(CompileContextNW context) {
			return new ByteString(toString(), CAOSUtils.CAOS_CHARSET);
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	public static class Flo extends Number {
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
		public float toFloat() {
			return value;
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
		public ByteString getInlineCAOSConst(CompileContextNW context) {
			return new ByteString(toString(), CAOSUtils.CAOS_CHARSET);
		}

		@Override
		public String toString() {
			return toCAOSFloat(value);
		}

		/**
		 * Converts a float in a CAOS-compatible way.
		 */
		public static String toCAOSFloat(float f) {
			if (!Float.isFinite(f))
				throw new RuntimeException("Cannot represent non-finite float " + f + " in CAOS");
			// THE FOLLOWING CODE IS HIGHLY DEPENDENT ON FOLLOWING THE JAVA PLATFORM SE 7 DEFINITION OF Float.toString(float)!
			// Which, to be clear, is fine, since breaking compatibility on this would be bad, but still.
			String s = Float.toString(f);
			// We only need to do any recovery if scientific notation was used.
			int botchedLoc = s.indexOf('E');
			if (botchedLoc == -1)
				return s;
			String sanePart = s.substring(0, botchedLoc);
			int adjustment = Integer.valueOf(s.substring(botchedLoc + 1));
			int sanePartDotLoc = s.indexOf('.');
			String left = sanePart.substring(0, sanePartDotLoc);
			String right = sanePart.substring(sanePartDotLoc + 1);
			String sign = "";
			if (left.startsWith("-")) {
				sign = "-";
				left = left.substring(1);
			}
			while (adjustment < 0) {
				// transfer from left to right
				right = left.substring(left.length() - 1) + right;
				left = left.substring(0, left.length() - 1);
				if (left.equals(""))
					left = "0";
				adjustment++;
			}
			while (adjustment > 0) {
				// transfer from right to left
				left = left + right.substring(0, 1);
				right = right.substring(1, right.length());
				if (right.equals(""))
					right = "0";
				adjustment--;
			}
			while ((right.length() > 1) && right.endsWith("0"))
				right = right.substring(0, right.length() - 1);
			return sign + left + "." + right;
		}
	}

	public static class Bytes extends Single {
		private final byte[] value;
		public Bytes(TypeSystem ts, byte[] d) {
			super(ts.gBytes);
			value = d;
		}

		public Bytes(RALType rt, byte[] d) {
			super(rt);
			value = d;
		}

		@Override
		public Bytes cast(RALType rt) {
			return new Bytes(rt, value);
		}

		// don't bother with equality checks, it's not worth it

		@Override
		public ByteString getInlineCAOSConst(CompileContextNW context) {
			return new ByteString(toString(), CAOSUtils.CAOS_CHARSET);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("[ ");
			for (byte b : value) {
				sb.append(b & 0xFF);
				sb.append(" ");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	public static class Callable extends Single {
		public final RALCallable value;
		public Callable(RALType rt, RALCallable d) {
			super(rt);
			value = d;
		}

		@Override
		public String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			return null;
		}

		@Override
		protected void readCompileInner(RALExprSlice out, CompileContext context) {
			throw new RuntimeException("Lambdas and macros are not real values");
		}

		@Override
		protected RALCallable getCallableInner(int index) {
			return value;
		}

		@Override
		public Callable cast(RALType rt) {
			return new Callable(rt, value);
		}

		@Override
		public ByteString getInlineCAOSConst(CompileContextNW context) {
			return null;
		}

		@Override
		public String toString() {
			return "(lambda)";
		}
	}
}
