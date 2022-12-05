/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.io.Reader;
import java.util.LinkedList;

import rals.code.CompileContext;
import rals.debug.DebugSite;

/**
 * Magic.
 */
public class ProcessedDebugFrame {
	public final String name;
	public final String lineIdentifier;
	public final RawDebugFrame base;
	public final String[] vaNames;
	public final CodeViewPane.Contents contents;
	public final boolean shouldAvoid;

	public ProcessedDebugFrame(String n, String li, RawDebugFrame b, String[] v, CodeViewPane.Contents c, boolean ic) {
		name = n;
		lineIdentifier = li;
		base = b;
		vaNames = v;
		contents = c;
		shouldAvoid = ic;
	}

	@Override
	public String toString() {
		return name;
	}

	private static String[] getVANames() {
		String[] vaNames = new String[100];
		for (int i = 0; i < vaNames.length; i++)
			vaNames[i] = CompileContext.vaToString(i);
		return vaNames;
	}
	public static ProcessedDebugFrame[] process(RawDebugFrame rdf) {
		CodeViewPane.Contents caosContents = findLineInCAOS(rdf.caos, rdf.caosOffset);
		String caosLI = "#" + rdf.caosOffset;
		ProcessedDebugFrame caosFrame = new ProcessedDebugFrame("CAOS", caosLI, rdf, getVANames(), caosContents, true);
		LinkedList<ProcessedDebugFrame> frames = new LinkedList<>();
		DebugSite ds = DebugSite.tryDecode(rdf.va[99]);
		while (ds != null) {
			// process individual frame
			try {
				// this is bad!
				String fileStr = ds.location.file.toString();
				boolean likelyStdLib = false;
				if (fileStr.contains("/std/") || fileStr.contains("\\std\\"))
					likelyStdLib = true;
				Reader r = ds.location.file.open();
				StringBuilder sb = new StringBuilder();
				while (true) {
					int ch = r.read();
					if (ch == -1)
						break;
					sb.append((char) ch);
				}
				String[] vaNames = getVANames();
				for (int i = 0; i < vaNames.length; i++)
					if (ds.vaNames[i] != null)
						vaNames[i] = ds.vaNames[i];
				CodeViewPane.Contents nfc = new CodeViewPane.Contents(sb.toString(), ds.location.line, ds.location.character);
				String li = ds.location.file.toString() + "#" + ds.location.line;
				frames.add(new ProcessedDebugFrame(ds.location.toString(), li, rdf, vaNames, nfc, likelyStdLib));
			} catch (Exception ex) {
			}
			ds = ds.parent;
		}
		frames.add(caosFrame);
		return frames.toArray(new ProcessedDebugFrame[0]);
	}
	private static CodeViewPane.Contents findLineInCAOS(String caos, int ofs) {
		int plannedLine = 0;
		int chr = 0;
		if (ofs < caos.length()) {
			for (int i = 0; i < ofs; i++) {
				if (caos.charAt(i) == 10) {
					plannedLine++;
					chr = 0;
				} else {
					chr++;
				}
			}
		}
		return new CodeViewPane.Contents(caos, plannedLine, chr);
	}
}
