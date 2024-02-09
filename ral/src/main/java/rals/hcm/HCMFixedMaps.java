/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.HashMap;

import rals.code.CodeGenFeatureLevel;
import rals.hcm.HCMStorage.HoverData;
import rals.lex.DefInfo;

public class HCMFixedMaps {
	public static final HashMap<String, HoverData> DECLARATIONS = new HashMap<>();
	static {
		put(DECLARATIONS, "include", "Includes a file, if it has not been included already.");
		put(DECLARATIONS, "addSearchPath", "Adds a directory to the include search path.");
		put(DECLARATIONS, "class", "Defines a class.");
		put(DECLARATIONS, "interface", "Defines an interface.");
		put(DECLARATIONS, "typedef", "Defines a typedef.");
		put(DECLARATIONS, "field", "Defines a field on an agent type/interface.");
		put(DECLARATIONS, "message", "Defines a message number on an agent type/interface.");
		put(DECLARATIONS, "script", "Defines a script number on an agent type/interface, or defines the code of a script.");
		put(DECLARATIONS, "install", "Adds install code.");
		put(DECLARATIONS, "remove", "Adds remove code.");
		put(DECLARATIONS, "macro", "Defines a macro.");
		put(DECLARATIONS, "overrideOwnr", "Indicates a script number to have non-standard `ownr` semantics.");
		put(DECLARATIONS, "messageHook", "Indicates separated script/message number.");
		put(DECLARATIONS, "assertConst", "Asserts an expression must result in a constant.");
		put(DECLARATIONS, "codeGenFeatureLevel", "Changes the codegen feature level used for the whole project.");
		put(DECLARATIONS, ";", "Does nothing.");
	}

	public static final HashMap<String, HoverData> CODEGEN_LEVELS = new HashMap<>();
	static {
		for (CodeGenFeatureLevel v : CodeGenFeatureLevel.values()) {
			String name = v.name();
			put(CODEGEN_LEVELS, name, v.defInfo);
		}
	}

	private static void put(HashMap<String, HoverData> map, String name, String text) {
		map.put(name, new HoverData(name, new DefInfo.Builtin(text)));
	}
}
