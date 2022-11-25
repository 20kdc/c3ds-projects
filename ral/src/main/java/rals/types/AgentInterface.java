/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

import java.util.HashMap;

import rals.lex.DefInfo;

/**
 * AgentInterface represents a set of exposed object variables and messages.
 * Notably, this is simply the point for insertion at a particular point - RALTypes make up the "full story".
 */
public final class AgentInterface {
	/**
	 * toString of this is the name. Does NOT have a concrete meaning, is just used for HCM right now.
	 * This is important as it makes Interfaces and renamed Classifiers work in HCM field/message/script lookups.
	 */
	public final Object nameGiver;

	public final HashMap<String, Integer> messages = new HashMap<>();
	public final HashMap<Integer, String> messagesInv = new HashMap<>();
	public final HashMap<String, Integer> scripts = new HashMap<>();
	public final HashMap<Integer, String> scriptsInv = new HashMap<>();

	public final HashMap<String, OVar> fields = new HashMap<>();

	public AgentInterface(Object n) {
		nameGiver = n;
	}

	public static class OVar {
		public final int slot;
		public final RALType type;
		public final DefInfo defInfo;

		public OVar(int s, RALType t, DefInfo di) {
			slot = s;
			type = t;
			defInfo = di;
		}
	}
}
