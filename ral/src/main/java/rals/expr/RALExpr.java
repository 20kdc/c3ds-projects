/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.CompileContext;
import rals.code.IVAAllocator;
import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * Expression. Outputs a given set of types.
 */
public interface RALExpr {
	/**
	 * What types can be read?
	 * Also used to test readability (throws exception if not readable)
	 */
	RALType[] outTypes();

	/**
	 * Just get the typechecking over with already
	 */
	default RALType assertOutTypeSingle() {
		RALType[] rt = outTypes();
		if (rt.length != 1)
			throw new RuntimeException("Expected single got " + this);
		return rt[0];
	}

	/**
	 * Just get the typechecking over with already
	 */
	default RALType assertOutTypeSingleImpcast(RALType target) {
		RALType[] rt = outTypes();
		if (rt.length != 1)
			throw new RuntimeException("Expected single " + target + " here");
		rt[0].implicitlyCastOrThrow(target);
		return rt[0];
	}

	/**
	 * Compiles this expression, which writes into the given output expressions.
	 * This is done by calling their inCompile methods, or in some cases using getInlineCAOS.
	 */
	void outCompile(StringBuilder writer, RALExpr[] out, CompileContext context);

	/**
	 * What type can be written?
	 * Also used to test writability (throws exception if not writable)
	 */
	RALType inType();

	/**
	 * Compiles a write.
	 * WARNING: May alter TARG before input runs. If this matters, make a temporary.
	 */
	void inCompile(StringBuilder writer, String input, RALType inputExactType, CompileContext context);

	/**
	 * Gets the inline CAOS for this expression, or null if that's not possible.
	 * This acts as a "fast-path" to avoid temporary variables.
	 * It's also critical to how inline statements let you modify variables, hence the name.
	 */
	default String getInlineCAOS(CompileContext context) {
		return getSpecialInline(context).code;
	}

	/**
	 * Like getInlineCAOS but better.
	 */
	default SpecialInline getSpecialInline(CompileContext context) {
		return SpecialInline.None;
	}

	/**
	 * This is similar to getInlineCAOS, but it's for very specific circumstances.
	 */
	public enum SpecialInline {
		None(null),
		Ownr("ownr"),
		Targ("targ");
		public final String code;
		SpecialInline(String s) {
			code = s;
		}
	}
}
