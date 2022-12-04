/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;

/**
 * Records compile errors and so forth.
 */
public class DiagRecorder {
	public boolean hasFailed = false;
	public LinkedList<Diag> diagnostics = new LinkedList<>();
	/**
	 * Stack frames of diag business.
	 * Items are added/removed from this in, for example, RALStatement.resolve.
	 * Use push/pop.
	 */
	private LinkedList<SrcRange> frames = new LinkedList<>();

	public void diag(Diag d) {
		if (d.kind == Diag.Kind.Error)
			hasFailed = true;
		diagnostics.add(d);
	}

	/**
	 * Lexer/Parser errors need to use this because:
	 * 1. they're very immediate
	 * 2. deliberately discarding frame data in favour of the dedicated include error mechanism is important here
	 */
	public void lexParseErr(SrcPos sp, String text) {
		lexParseErr(sp.toRange(), text);
	}

	public void lexParseErr(SrcPos sp, String text, Exception ex) {
		lexParseErr(sp.toRange(), text, ex);
	}

	public void lexParseErr(SrcRange sp, String text) {
		frames.push(sp);
		error(text);
		frames.pop();
	}

	public void lexParseErr(SrcRange sp, String text, Exception ex) {
		frames.push(sp);
		error(text, ex);
		frames.pop();
	}

	public void error(String text) {
		diag(new Diag(Diag.Kind.Error, frames.toArray(new SrcRange[0]), text, text));
	}

	public void error(Exception ex) {
		error("", ex);
	}

	public void error(String text, Exception ex) {
		StringWriter details = new StringWriter();
		details.append(text);
		ex.printStackTrace(new PrintWriter(details));
		diag(new Diag(Diag.Kind.Error, frames.toArray(new SrcRange[0]), details.toString(), ex.getMessage()));
	}

	public String unwrapToString() {
		if (hasFailed) {
			StringBuilder sb = new StringBuilder();
			for (Diag d : diagnostics) {
				sb.append(d.frames[0]);
				sb.append(": ");
				sb.append(d.shortText);
				sb.append("\n");
			}
			sb.append("\n -- Long Versions... --\n\n");
			for (Diag d : diagnostics) {
				for (SrcRange sr : d.frames) {
					sb.append(sr);
					sb.append(": ");
				}
				sb.append(d.text);
				sb.append("\n");
			}
			return sb.toString();
		}
		return null;
	}

	public void unwrap() {
		String s = unwrapToString();
		if (s != null)
			throw new RuntimeException("Compile errors:\n" + s);
	}

	public void pushFrame(SrcRange extent) {
		frames.push(extent);
	}

	public void popFrame(SrcRange extent) {
		SrcRange e2 = frames.pop(); 
		if (e2 != extent) {
			// put it back so that things don't get worse, maybe
			frames.push(e2);
			throw new RuntimeException("FRAME MISMATCH IN DIAG: " + extent + " vs " + e2);
		}
	}

	public Scope newScope(final SrcRange extent) {
		pushFrame(extent);
		return new Scope() {
			@Override
			public void close() {
				popFrame(extent);
			}
		};
	}

	public static interface Scope extends AutoCloseable {
		@Override
		public void close();
	}
}
