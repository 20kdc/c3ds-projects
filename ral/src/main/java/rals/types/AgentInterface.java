/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

import java.util.HashMap;

import rals.caos.CAOSUtils;
import rals.lex.DefInfo;

/**
 * AgentInterface represents a set of exposed object variables and messages.
 * Notably, this is simply the point for insertion at a particular point - RALTypes make up the "full story".
 */
public final class AgentInterface {
	/**
	 * The canonical (defining) type of the interface.
	 * This is important as it makes Interfaces and renamed Classifiers work in HCM field/message/script lookups.
	 * This is also important for documentation stuff.
	 */
	public final RALType canonicalType;

	public final HashMap<String, MsgScr> messages = new HashMap<>();
	public final HashMap<Integer, String> messagesInv = new HashMap<>();
	public final HashMap<String, MsgScr> scripts = new HashMap<>();
	public final HashMap<Integer, String> scriptsInv = new HashMap<>();

	public final HashMap<String, OVar> fields = new HashMap<>();

	public AgentInterface(RALType ct) {
		canonicalType = ct;
	}

	/**
	 * Base class so that DocGen doesn't get silly.
	 */
	public static class Attachment implements Comparable<Attachment> {
		public final String name;
		public final DefInfo defInfo;
		public Attachment(String n, DefInfo di) {
			name = n;
			defInfo = di;
		}
		@Override
		public int compareTo(Attachment var1) {
			return name.compareTo(var1.name);
		}
	}

	public static class OVar extends Attachment {
		public final int slot;
		public final RALType type;

		public OVar(String n, int s, RALType t, DefInfo di) {
			super(n, di);
			slot = s;
			type = t;
		}

		@Override
		public String toString() {
			return "field " + type + " " + name + " (" + CAOSUtils.vaToString("ov", slot) + ")";
		}
	}

	public static class MsgScr extends Attachment {
		public final MST intent;
		public final int value;

		public MsgScr(String n, int v, MST i, DefInfo di) {
			super(n, di);
			intent = i;
			value = v;
		}

		@Override
		public String toString() {
			if (intent == MST.Message)
				return "msg " + name + " (" + value + ")";
			if (intent == MST.Script)
				return "scr " + name + " (" + value + ")";
			return "m/s " + name + " (" + value + ")";
		}
	}

	public static enum MST {
		Message,
		Script,
		Both
	}
}
