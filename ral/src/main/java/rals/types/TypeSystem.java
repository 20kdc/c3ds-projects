/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import rals.expr.RALConstant;
import rals.expr.RALExprGroup;
import rals.expr.RALExprUR;
import rals.types.RALType.AgentClassifier;
import rals.types.RALType.Opaque;

/**
 * Types (including the Scriptorium).
 * Where RALType's code represents the rules of the types, TypeSystem represents the management of those types.
 */
public class TypeSystem {
	public final Opaque gAny = new RALType.Any();
	public final Opaque gString = new Opaque(RALType.Major.String, "string");
	public final Opaque gInteger = new Opaque(RALType.Major.Value, "integer");
	public final Opaque gFloat = new Opaque(RALType.Major.Value, "float");
	public final Opaque gNull = new Opaque(RALType.Major.Agent, "null");
	public final Opaque gVoid = new Opaque(RALType.Major.Unknown, "void");
	public final RALType.AgentClassifier gAgent = new RALType.AgentClassifier(new Classifier(0, 0, 0), null);
	public final RALType gAgentNullable;

	/**
	 * All agent types by classifier.
	 * Essentially the Scriptorium in a box.
	 */
	public final HashMap<Classifier, RALType.AgentClassifier> classifiers = new HashMap<>();

	public final HashMap<String, RALType> namedTypes = new HashMap<>();

	/**
	 * All agent interfaces by name.
	 * This is for declaration only, as the ultimate reference remains the Agent Type hierarchy.
	 */
	public final HashMap<String, AgentInterface> namedInterfaces = new HashMap<>();

	/**
	 * All unions by their components.
	 */
	public final HashMap<HashSet<RALType>, RALType.Union> unions = new HashMap<>();

	/**
	 * Named constants!
	 */
	public final HashMap<String, RALConstant> namedConstants = new HashMap<>();

	public TypeSystem() {
		namedTypes.put("any", gAny);
		namedTypes.put("string", gString);
		namedTypes.put("integer", gInteger);
		namedTypes.put("float", gFloat);
		namedTypes.put("null", gNull);
		// setup Agent type
		classifiers.put(gAgent.classifier, gAgent);
		gAgent.typeName = "Agent";
		namedTypes.put("Agent", gAgent);
		gAgentNullable = byNullable(gAgent);
	}

	/**
	 * Gets an agent type by classifier.
	 */
	public RALType.AgentClassifier byClassifier(Classifier cl) {
		RALType.AgentClassifier rt = classifiers.get(cl);
		if (rt != null)
			return rt;
		if (!cl.isScriptValid())
			throw new RuntimeException("Not valid script classifier: " + cl);
		Classifier clParent = cl.genScriptParent();
		RALType.AgentClassifier agParent = null;
		if (clParent != null)
			agParent = byClassifier(clParent);
		rt = new RALType.AgentClassifier(cl, agParent);
		classifiers.put(cl, rt);
		return rt;
	}

	/**
	 * Gets a type by name.
	 */
	public RALType byName(String name) {
		RALType rt = namedTypes.get(name);
		if (rt == null)
			throw new RuntimeException("No type " + name);
		return rt;
	}

	/**
	 * Gets a union type by contents.
	 * Note that this automatically flattens existing unions.
	 */
	public RALType byUnion(Iterable<RALType> in) {
		HashSet<RALType> types = new HashSet<>();
		for (RALType rt : in) {
			if (rt instanceof RALType.Union) {
				// flatten
				for (RALType rt2 : (RALType.Union) rt) {
					types.add(rt2);
				}
			} else {
				types.add(rt);
			}
		}
		HashSet<RALType> typesCopy = new HashSet<>(types);
		for (final RALType rt : typesCopy) {
			// Remove inferior types that aren't this type.
			types.removeIf((target) -> target != rt && target.canImplicitlyCast(rt));
		}
		int ts = types.size();
		if (ts == 1) {
			// single-type union? nuh.
			return types.iterator().next();
		} else if (ts == 0) {
			// impossible
			return gVoid;
		}
		// -- 'types' value finalized past here --
		// check for existing union
		RALType.Union res = unions.get(types);
		if (res != null)
			return res;
		// this should be the only reference to this constructor
		res = new RALType.Union(types);
		unions.put(types, res);
		return res;
	}

	public RALType byNullable(RALType t) {
		return byUnion(Arrays.asList(t, gNull));
	}

	public RALType byNonNullable(RALType t) {
		if (t instanceof RALType.Union) {
			LinkedList<RALType> res = new LinkedList<>();
			for (RALType rt : (RALType.Union) t)
				if (rt != gNull)
					res.add(rt);
			return byUnion(res);
		}
		throw new RuntimeException("Can't remove the null option from a non-union.");
	}

	/**
	 * Declares an agent class.
	 */
	public RALType.AgentClassifier declareClass(Classifier cl, String name) {
		RALType ort = namedTypes.get(name);
		RALType.AgentClassifier ag = byClassifier(cl);
		// already declared?
		if (ort == ag)
			return ag;
		// nevermind then
		checkConflictType(name);
		checkConflictInterface(name);
		namedTypes.put(name, ag);
		namedInterfaces.put(name, ag.inherent);
		ag.typeName = name;
		return ag;
	}

	/**
	 * Declares an agent interface.
	 */
	public RALType.Agent declareInterface(String name) {
		RALType ort = namedTypes.get(name);
		// already declared?
		if (ort != null)
			if (ort instanceof RALType.Agent)
				return (RALType.Agent) ort;
		// nevermind then
		checkConflictType(name);
		checkConflictInterface(name);
		RALType.Agent ag = new RALType.Agent(name);
		namedTypes.put(name, ag);
		namedInterfaces.put(name, ag.inherent);
		return ag;
	}

	private void checkConflictType(String name) {
		if (namedTypes.containsKey(name))
			throw new RuntimeException("Type conflict: " + name);
	}
	private void checkConflictInterface(String name) {
		if (namedInterfaces.containsKey(name))
			throw new RuntimeException("Interface conflict: " + name);
	}

	public void declareConst(String name, RALConstant cst) {
		if (namedConstants.containsKey(name))
			throw new RuntimeException("Constant conflict: " + name);
		namedConstants.put(name, cst);
	}

	public AgentClassifier tryGetAsClassifier(String text) {
		RALType maybeClassifier = namedTypes.get(text);
		if (maybeClassifier instanceof RALType.AgentClassifier)
			return (RALType.AgentClassifier) maybeClassifier;
		return null;
	}

	public void declareTypedef(String name, RALType parseType) {
		RALType existing = namedTypes.get(name);
		if (existing != null && existing != parseType)
			throw new RuntimeException("Can't redeclare type " + name);
	}
}
