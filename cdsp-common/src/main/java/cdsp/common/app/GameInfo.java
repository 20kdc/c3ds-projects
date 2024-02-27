/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import cdsp.common.data.DirLookup;

/**
 * Game information.
 */
public class GameInfo implements DirLookup {
	/**
	 * Character set of the game.
	 */
	public Charset charset;

	/**
	 * Locations indexed by location.
	 */
	public final EnumMap<Location, LinkedList<File>> locations = new EnumMap<>(Location.class);

	public GameInfo() {
		charset = StandardCharsets.ISO_8859_1;
		for (Location loc : Location.values())
			locations.put(loc, new LinkedList<>());
	}

	/**
	 * Tries to do some magic.
	 */
	public void fromGameDirectory(File dir) {
		for (Location loc : Location.values()) {
			File f = new File(dir, loc.nameTypical);
			if (f.exists())
				locations.get(loc).add(f);
		}
	}

	/**
	 * Load configuration from JSON.
	 */
	public void load(Object jsonObj) {
		try {
			if (jsonObj instanceof JSONObject) {
				charset = Charset.forName(((JSONObject) jsonObj).getString("charset"));
				JSONObject ljo = ((JSONObject) jsonObj).getJSONObject("locations");
				for (Location loc : Location.values()) {
					JSONArray locArr = ljo.optJSONArray(loc.nameInternal);
					if (locArr != null) {
						LinkedList<File> llf = locations.get(loc);
						int len = locArr.length();
						for (int i = 0; i < len; i++) {
							String str = locArr.getString(i);
							llf.add(new File(str));
						}
					}
				}
			}
		} catch (Exception je) {
			je.printStackTrace();
		}
	}

	/**
	 * Loads from the default location.
	 */
	public void loadFromDefaultLocation() {
		load(AppConfig.load("gameinfo.json"));
	}

	/**
	 * Turns this configuration into a JSONObject.
	 */
	public JSONObject save() {
		JSONObject obj = new JSONObject();
		obj.put("charset", charset.name());
		JSONObject loc = new JSONObject();
		for (Map.Entry<Location, LinkedList<File>> ent : locations.entrySet()) {
			JSONArray array = new JSONArray();
			for (File f : ent.getValue())
				array.put(f.toString());
			loc.put(ent.getKey().nameInternal, array);
		}
		obj.put("locations", loc);
		return obj;
	}

	/**
	 * Saves to the default location.
	 */
	public void saveToDefaultLocation() {
		AppConfig.save("gameinfo.json", save());
	}

	/**
	 * Returns the location of a file.
	 */
	@Override
	public File findFile(Location location, String name) {
		for (File f : locations.get(location)) {
			File potential = new File(f, name);
			if (potential.exists())
				return potential;
		}
		return newFile(location, name);
	}

	/**
	 * Returns the location where a file would go.
	 */
	public File newFile(Location location, String name) {
		return new File(locations.get(location).getFirst(), name);
	}
}
