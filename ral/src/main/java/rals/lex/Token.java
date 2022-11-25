/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.lex;

import java.util.HashSet;

import rals.diag.SrcPos;
import rals.diag.SrcRange;
import rals.parser.IDocPath;

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
		keywords.add("instanceof");
		keywords.add("with");
		keywords.add("after");
		keywords.add("for");
		keywords.add("foreach");
		keywords.add("in");
		keywords.add("call");
	}

	public final SrcPos lineNumber;
	public final SrcRange extent;
	public String docComment;

	public Token(SrcRange ex, String doc) {
		lineNumber = ex.start;
		extent = ex;
		docComment = doc;
	}

	public boolean isInDP(IDocPath docPath) {
		return extent.isInDP(docPath);
	}

	public boolean isKeyword(String kw) {
		if (this instanceof Kw)
			if (((Kw) this).text.equals(kw))
				return true;
		return false;
	}

	public static class ID extends Token {
		public String text;
		public ID(SrcRange ln, String dc, String tx) {
			super(ln, dc);
			text = tx;
		}

		@Override
		public String toString() {
			return "ID:" + text;
		}
	}
	public static class Kw extends Token {
		public String text;
		public Kw(SrcRange ln, String dc, String tx) {
			super(ln, dc);
			text = tx;
		}

		@Override
		public String toString() {
			return "K:" + text;
		}
	}
	public static class Str extends Token {
		public String text;
		public Str(SrcRange ln, String dc, String tx) {
			super(ln, dc);
			text = tx;
		}

		@Override
		public String toString() {
			return "S:" + text;
		}
	}
	public static class StrEmb extends Token {
		public String text;
		public boolean startIsClusterEnd, endIsClusterStart;
		public StrEmb(SrcRange ln, String dc, String tx, boolean ce, boolean cs) {
			super(ln, dc);
			text = tx;
			startIsClusterEnd = ce;
			endIsClusterStart = cs;
		}

		@Override
		public String toString() {
			return "SE:" + text;
		}
	}
	public static class Int extends Token {
		public int value;
		public Int(SrcRange ln, String dc, int v) {
			super(ln, dc);
			value = v;
		}

		@Override
		public String toString() {
			return "I:" + value;
		}
	}
	public static class Flo extends Token {
		public float value;
		public Flo(SrcRange ln, String dc, float v) {
			super(ln, dc);
			value = v;
		}

		@Override
		public String toString() {
			return "F:" + value;
		}
	}
}
