/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import cdsp.common.data.DirLookup;
import cdsp.common.data.bytestring.W1252Fixed;

/**
 * Game information.
 */
public class GameInfo implements DirLookup {
	/**
	 * File version (used for upgrades)
	 */
	public static final int VERSION = 1;

	/**
	 * Character set of the game.
	 */
	public Charset charset;

	/**
	 * Locations indexed by location.
	 */
	public final EnumMap<Location, LinkedList<File>> locations = new EnumMap<>(Location.class);

	public GameInfo() {
		for (Location loc : Location.values())
			locations.put(loc, new LinkedList<>());
		reset();
	}

	/**
	 * Does a full reset.
	 */
	public void reset() {
		charset = W1252Fixed.INSTANCE;
		for (LinkedList<File> llf : locations.values())
			llf.clear();
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
		reset();
		try {
			if (jsonObj instanceof JSONObject) {
				JSONObject jo = (JSONObject) jsonObj;
				int version = jo.optInt("version", 0);
				try {
					String charsetName = jo.getString("charset");
					if (charsetName.equals("W1252Fixed")) {
						charset = W1252Fixed.INSTANCE;
					} else {
						charset = Charset.forName(charsetName);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				if (version == 0 && charset.name().equals("ISO-8859-1"))
					charset = W1252Fixed.INSTANCE;
				JSONObject ljo = jo.optJSONObject("locations");
				if (ljo != null) {
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
		obj.put("version", VERSION);
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
		FilenameFilter caseInsensitiveChecker = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String chkName) {
				return chkName.equalsIgnoreCase(name);
			}
		};
		for (File f : locations.get(location)) {
			File potential = new File(f, name);
			if (potential.exists())
				return potential;
			File[] targets = f.listFiles(caseInsensitiveChecker);
			if (targets != null)
				if (targets.length != 0)
					if (targets[0] != null)
						return targets[0];
		}
		return newFile(location, name);
	}

	@Override
	public File[] listFiles(Location location) {
		HashSet<String> includedCanonized = new HashSet<>();
		LinkedList<File> list = new LinkedList<>();
		for (File f : locations.get(location)) {
			File[] targets = f.listFiles();
			if (targets == null)
				continue;
			for (File s : targets) {
				String sCanonized = s.getName().toLowerCase();
				if (includedCanonized.add(sCanonized))
					list.add(s);
			}
		}
		return list.toArray(new File[0]);
	}

	/**
	 * Returns the location where a file would go.
	 */
	public File newFile(Location location, String name) {
		return new File(locations.get(location).getFirst(), name);
	}

	public boolean looksEmpty() {
		return locations.get(Location.GENETICS).size() == 0;
	}
}
