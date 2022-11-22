/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.diag;

import org.json.JSONObject;

/**
 * Position in source code.
 * This is represented in language server format.
 */
public class SrcPos {
	/**
	 * The file identifier of this source position.
	 * It would be nice if this was cleaner.
	 */
	public final SrcPosFile file;

	/**
	 * The global index of the source position.
	 * This is EXACTLY equivalent to indexes that would be passed to a charAt on the String of a StringReader.
	 */
	public final int globalPosition;
	public final int line, character;
	public SrcPos(SrcPosFile f, int g, int l, int c) {
		file = f;
		globalPosition = g;
		line = l;
		character = c;
	}

	@Override
	public String toString() {
		return file + ":" + (line + 1) + "," + (character + 1);
	}

	/**
	 * Translates this to a Language Server Protocol UTF-16 range.
	 */
	public JSONObject toLSPPosition() {
		return new JSONObject("{\"line\":" + line + ",\"character\":" + character + "}"); 
	}

	/**
	 * Translates this to a Language Server Protocol UTF-16 range.
	 */
	public JSONObject toLSPRange() {
		JSONObject range = new JSONObject();
		JSONObject pos = toLSPPosition();
		range.put("start", pos);
		range.put("end", pos);
		return range;
	}
}
