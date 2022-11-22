/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

import org.json.JSONObject;

/**
 * Range in source code.
 */
public class SrcRange {
	public final SrcPosFile file;
	public final SrcPos start, end;
	public SrcRange(SrcPos s, SrcPos e) {
		if (s.file != e.file)
			throw new RuntimeException("Range start and end must have same SrcPosFile");
		file = s.file;
		start = s;
		end = e;
	}

	/**
	 * Translates this to a Language Server Protocol Range.
	 */
	public JSONObject toLSPRange() {
		JSONObject range = new JSONObject();
		range.put("start", start.toLSPPosition());
		range.put("end", end.toLSPPosition());
		return range;
	}
}
