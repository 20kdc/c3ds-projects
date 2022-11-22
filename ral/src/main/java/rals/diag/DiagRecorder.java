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

	public void diag(Diag d) {
		if (d.kind == Diag.Kind.Error)
			hasFailed = true;
		diagnostics.add(d);
	}

	public void error(SrcPos where, String text) {
		diag(new Diag(Diag.Kind.Error, where, text, text));
	}

	public void error(SrcRange where, String text) {
		diag(new Diag(Diag.Kind.Error, where, text, text));
	}

	public void error(SrcPos where, Exception ex) {
		error(where, "", ex);
	}

	public void error(SrcRange where, Exception ex) {
		error(where, "", ex);
	}

	public void error(SrcPos where, String text, Exception ex) {
		error(new SrcRange(where, where), text, ex);
	}

	public void error(SrcRange where, String text, Exception ex) {
		StringWriter details = new StringWriter();
		details.append(text);
		ex.printStackTrace(new PrintWriter(details));
		diag(new Diag(Diag.Kind.Error, where, details.toString(), ex.getMessage()));
	}

	public void unwrap() {
		if (hasFailed) {
			StringBuilder sb = new StringBuilder();
			sb.append("Compile errors:\n");
			for (Diag d : diagnostics) {
				sb.append(d.location);
				sb.append(": ");
				sb.append(d.text);
				sb.append("\n");
			}
			throw new RuntimeException(sb.toString());
		}
	}
}
