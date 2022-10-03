/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.util.HashSet;

/**
 * Text (may not be text) with a type and a position. 
 */
public class Token {
	public static final HashSet<String> keywords = new HashSet<>();
	static {
		// meta
		keywords.add("include");
		keywords.add("addSearchPath");
		// type system declarations
		keywords.add("class");
		keywords.add("interface");
		keywords.add("extends");
		keywords.add("typedef");
		keywords.add("message");
		keywords.add("field");
		keywords.add("assertConst");
		// evil decls
		keywords.add("overrideOwnr");
		keywords.add("messageHook");
		// code declarations
		keywords.add("macro");
		keywords.add("inline");
		keywords.add("script");
		keywords.add("install");
		keywords.add("remove");
		// in-function
		keywords.add("let");
		keywords.add("if");
		keywords.add("else");
		keywords.add("while");
		keywords.add("break");
		keywords.add("alias");
		keywords.add("return");
		keywords.add("assertType");
		keywords.add("after");
		keywords.add("foreach");
		keywords.add("in");
	}

	public final SrcPos lineNumber;
	public Token(SrcPos ln) {
		lineNumber = ln;
	}

	public boolean isKeyword(String kw) {
		if (this instanceof Kw)
			if (((Kw) this).text.equals(kw))
				return true;
		return false;
	}

	public static class ID extends Token {
		public String text;
		public ID(SrcPos ln, String tx) {
			super(ln);
			text = tx;
		}

		@Override
		public String toString() {
			return "ID:" + lineNumber + ":" + text;
		}
	}
	public static class Kw extends Token {
		public String text;
		public Kw(SrcPos ln, String tx) {
			super(ln);
			text = tx;
		}

		@Override
		public String toString() {
			return "Kw:" + lineNumber + ":" + text;
		}
	}
	public static class Str extends Token {
		public String text;
		public Str(SrcPos ln, String tx) {
			super(ln);
			text = tx;
		}

		@Override
		public String toString() {
			return "Str:" + lineNumber + ":" + text;
		}
	}
	public static class Int extends Token {
		public int value;
		public Int(SrcPos ln, int v) {
			super(ln);
			value = v;
		}

		@Override
		public String toString() {
			return "Int:" + lineNumber + ":" + value;
		}
	}
	public static class Flo extends Token {
		public float value;
		public Flo(SrcPos ln, float v) {
			super(ln);
			value = v;
		}

		@Override
		public String toString() {
			return "Flo:" + lineNumber + ":" + value;
		}
	}
}
