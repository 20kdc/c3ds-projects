/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.debug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONObject;
import org.json.JSONTokener;

import rals.caos.CAOSUtils;
import rals.cctx.*;
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
		for (Map.Entry<IVAHandle, Integer> lv : cc.getVAHandleEntrySet())
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
			String df = CAOSUtils.vaToString(i);
			if (jo.has(df))
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
			String df = CAOSUtils.vaToString(i);
			if (vaNames[i] != null)
				jo.put(df, vaNames[i]);
		}
		return jo;
	}

	public String encode() {
		byte[] data = toJSON().toString().getBytes(StandardCharsets.UTF_8);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gos = new GZIPOutputStream(baos);
			gos.write(data);
			gos.close();
			data = baos.toByteArray();
		} catch (Exception e2) {
			throw new RuntimeException(e2);
		}
		return Base64.getEncoder().encodeToString(data);
	}

	public static DebugSite tryDecode(String string) {
		try {
			byte[] data = Base64.getDecoder().decode(string);
			try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data))) {
				return new DebugSite(new JSONObject(new JSONTokener(new InputStreamReader(gis, StandardCharsets.UTF_8))));
			}
		} catch (Exception ex) {
			return null;
		}
	}
}
