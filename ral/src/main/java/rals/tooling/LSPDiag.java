/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import rals.diag.SrcRange;

import org.json.JSONObject;

import rals.diag.Diag;
import rals.diag.Diag.Kind;

/**
 * Diag, but for LSP.
 */
public class LSPDiag {
	public final Kind kind;
	public final SrcRange extent;
	public final String text;

	public LSPDiag(Kind k, SrcRange ex, String t) {
		kind = k;
		extent = ex;
		text = t;
	}

	public LSPDiag(Diag d) {
		kind = d.kind;
		extent = d.frames[0];
		text = d.shortText;
	}

	public JSONObject toLSPDiagnostic() {
		JSONObject diagJ = new JSONObject();
		diagJ.put("range", extent.toLSPRange());
		// lite-xl needs this to not malfunction
		diagJ.put("severity", 1);
		diagJ.put("message", text);
		return diagJ;
	}
}
