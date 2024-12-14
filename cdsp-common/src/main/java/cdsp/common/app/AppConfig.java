/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

/**
 * Application configuration directory & friends
 */
public class AppConfig {
	private static final File configRoot = configRootPos();

	private static File configRootPos() {
		File userHome = new File(System.getProperty("user.home", "."));
		return new File(new File(new File(userHome, ".local"), "share"), "cdsp-common");
	}

	/**
	 * Loads a config file (or returns null).
	 */
	public static Object load(String name) {
		try {
			File f = new File(configRoot, name);
			if (!f.exists())
				return null;
			try (FileInputStream fis = new FileInputStream(f)) {
				return new JSONTokener(new InputStreamReader(fis, StandardCharsets.UTF_8)).nextValue();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Saves a config file (or at least tries to).
	 */
	public static void save(String name, Object val) {
		try {
			configRoot.mkdirs();
			File f = new File(configRoot, name);
			StringWriter sw = new StringWriter();
			if (val instanceof JSONObject) {
				((JSONObject) val).write(sw, 4, 0);
			} else {
				sw.write(JSONWriter.valueToString(val));
			}
			Files.write(f.toPath(), sw.toString().getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
