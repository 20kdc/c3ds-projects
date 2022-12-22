/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A change in approach.
 */
public class NCFConfigProvider implements IConfigProvider {
	public final File file;
	public final LinkedList<String> entryList = new LinkedList<>();
	public final HashMap<String, String> entries = new HashMap<>();
	public final HashMap<String, String> descriptions = new HashMap<>();
	public boolean needsSave = false;

	public NCFConfigProvider(String src) throws IOException {
		file = new File(src);
		try {
			try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
				LinkedList<String> tokens = new LinkedList<>();
				StringBuilder buf = new StringBuilder();
				int state = 0;
				while (true) {
					int chr = isr.read();
					if (chr == -1)
						break;
					if (state == 0) {
						// ready
						if (chr == '#') {
							state = 1;
						} else if (chr <= 32) {
							// do nothing
						} else if (chr == '"') {
							// string
							state = 2;
						} else {
							// id
							state = 4;
							buf.append((char) chr);
						}
					} else if (state == 1) {
						// comment
						if (chr == '\n')
							state = 0;
					} else if (state == 2) {
						// string
						if (chr == '\\') {
							state = 3;
						} else if (chr == '\"') {
							tokens.add(buf.toString());
							buf.setLength(0);
							state = 0;
						} else {
							buf.append((char) chr);
						}
					} else if (state == 3) {
						// string escape
						if (chr == 'n') {
							buf.append(10);
						} else {
							buf.append((char) chr);
						}
						state = 2;
					} else if (state == 4) {
						// id
						if (chr <= 32) {
							tokens.add(buf.toString());
							buf.setLength(0);
							state = 0;
						} else {
							buf.append((char) chr);
						}
					}
				}
				String key = null;
				for (String s : tokens) {
					if (key == null) {
						key = s;
					} else {
						entries.put(key, s);
						key = null;
					}
				}
				if (key != null)
					throw new IOException("Key without value in configuration file!!!");
			}
		} catch (FileNotFoundException fnfe) {
			needsSave = true;
		}
	}

	@Override
	public String configVisit(String name, String defaultVal, String description) {
		if (!descriptions.containsKey(name)) {
			descriptions.put(name, description);
			entryList.add(name);
		}
		String res = entries.get(name);
		if (res == null) {
			// This write is important - it sets things up for the later save.
			entries.put(name, defaultVal);
			needsSave = true;
			return defaultVal;
		}
		return res;
	}

	@Override
	public void configFinished() throws IOException {
		if (needsSave) {
			FileOutputStream os = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			osw.write("# Natsue Configuration File - UTF-8\n");
			osw.write("# '#' defines a comment until the end of the line.\n");
			osw.write("# Any non-whitespace character that is not '#' or '\"' begins an ID.\n");
			osw.write("# An ID is terminated by whitespace.\n");
			osw.write("# '\"' begins a string.\n");
			osw.write("# '\"' within a string and '\\' itself may be escaped by prefixing it with '\\'.\n");
			osw.write("# A string is terminated by an unescaped '\"'.\n");
			osw.write("# Strings and IDs are effectively equivalent.\n");
			osw.write("# The file is made up of pairs of keys and values. Either can be strings or IDs.\n");
			osw.write("# Booleans can be true or false, integers are written in decimal.\n");
			osw.write("\n");
			for (String s : entryList) {
				String des = descriptions.get(s);
				osw.write("# " + des.replaceAll("\n", "\n# ") + "\n");
				writeVal(osw, s);
				osw.write(" ");
				writeVal(osw, entries.get(s));
				osw.write("\n");
				osw.write("\n");
			}
			osw.write("\n");
			osw.close();
		}
	}

	private void writeVal(Writer w, String s) throws IOException {
		// determine if this is sufficiently ID-like
		char[] ch = s.toCharArray();
		boolean idLike = true;
		for (int i = 0; i < ch.length; i++) {
			char chr = ch[i];
			idLike = ((chr > 32) && (chr != '"') && (chr != '#') && (chr != '\\'));
			if (!idLike)
				break;
		}
		if (idLike) {
			w.write(s);
		} else {
			w.write('"');
			for (int i = 0; i < ch.length; i++) {
				char chr = ch[i];
				if ((chr == '\\') || (chr == '\"'))
					w.write('\\');
				w.write(chr);
			}
			w.write('"');
		}
	}
}
