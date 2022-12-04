/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.debug;

import java.io.File;
import java.util.Map;

import org.json.JSONObject;

import rals.code.CompileContext;
import rals.code.IVAHandle;
import rals.diag.SrcPosUntranslated;
import rals.parser.FileDocPath;

/**
 * Snapshot of debugging data.
 */
public class DebugSite {
	public final DebugSite parent;
	public final SrcPosUntranslated location;
	public final String[] vaNames = new String[100];

	public DebugSite(DebugSite p, SrcPosUntranslated loc, CompileContext cc) {
		parent = p;
		location = loc;
		if (parent != null) {
			for (int i = 0; i < vaNames.length; i++)
				vaNames[i] = parent.vaNames[i];
		} else {
			for (int i = 0; i < vaNames.length; i++)
				vaNames[i] = CompileContext.vaToString(i);
		}
		for (Map.Entry<IVAHandle, Integer> lv : cc.heldVAHandles.entrySet())
			vaNames[lv.getValue()] = lv.getKey().toString();
	}

	public DebugSite(JSONObject jo) {
		if (jo.has("parent")) {
			parent = new DebugSite(jo.getJSONObject("parent"));
		} else {
			parent = null;
		}
		location = new SrcPosUntranslated(new FileDocPath(new File(jo.getString("file"))), jo.getInt("line"), jo.getInt("character"));
		for (int i = 0; i < vaNames.length; i++) {
			String df = CompileContext.vaToString(i);
			vaNames[i] = jo.getString(df);
		}
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		if (parent != null)
			jo.put("parent", parent.toJSON());
		jo.put("file", ((FileDocPath) location.file).file.getAbsolutePath());
		jo.put("line", location.line);
		jo.put("character", location.character);
		for (int i = 0; i < vaNames.length; i++) {
			String df = CompileContext.vaToString(i);
			jo.put(df, vaNames[i]);
		}
		return jo;
	}
}
