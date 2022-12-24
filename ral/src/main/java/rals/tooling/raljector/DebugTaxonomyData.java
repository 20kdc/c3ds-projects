/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.util.HashMap;
import java.util.Map;

import rals.caos.CAOSUtils;
import rals.types.AgentInterface;
import rals.types.Classifier;
import rals.types.RALType;
import rals.types.RALType.AgentClassifier;
import rals.types.TypeSystem;

/**
 * Contains class and field name information.
 */
public class DebugTaxonomyData {
	private final HashMap<Classifier, Entry> entries = new HashMap<>();
	private final HashMap<String, Classifier> names = new HashMap<>();

	/**
	 * Extracts taxonomy information from the TypeSystem.
	 */
	public DebugTaxonomyData(TypeSystem ts) {
		for (AgentClassifier ent : ts.classifiers.values())
			entries.put(ent.classifier, new Entry(ent));
		for (Map.Entry<String, RALType> ent : ts.getAllNamedTypes()) {
			RALType rt = ent.getValue();
			if (rt instanceof AgentClassifier)
				names.put(ent.getKey(), ((AgentClassifier) rt).classifier);
		}
	}

	/**
	 * Gets the best entry for the given classifier. Can return null under exceptional circumstances.
	 */
	public Entry getBestEntry(Classifier cls) {
		while (cls != null) {
			Entry res = entries.get(cls);
			if (res != null)
				return res;
			cls = cls.genScriptParent();
		}
		// no entry for 0 0 0? oh well
		return null;
	}

	/**
	 * Gets a classifier by name or null on failure.
	 */
	public Classifier classifierByName(String name) {
		return names.get(name);
	}

	/**
	 * Gets all names as an Iterable.
	 */
	public Iterable<String> allNamesIterable() {
		return names.keySet();
	}

	public class Entry {
		public final String name;
		public final Classifier classifier;
		public final String[] objectVariables = new String[100];
		public Entry(AgentClassifier ac) {
			name = ac.typeName;
			classifier = ac.classifier;
			for (int i = 0; i < objectVariables.length; i++)
				objectVariables[i] = CAOSUtils.vaToString("ov", i);
			for (AgentInterface ai : ac.getInterfaces())
				for (AgentInterface.OVar v : ai.fields.values())
					objectVariables[v.slot] = v.name;
		}
	}
}
