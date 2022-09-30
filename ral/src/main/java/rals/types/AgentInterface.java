/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

import java.util.HashMap;

/**
 * AgentInterface represents a set of exposed object variables and messages.
 * Notably, this is simply the point for insertion at a particular point - RALTypes make up the "full story".
 */
public final class AgentInterface {
	public final HashMap<String, Integer> messages = new HashMap<>();
	public final HashMap<Integer, String> messagesInv = new HashMap<>();

	public AgentInterface() {
	}
}
