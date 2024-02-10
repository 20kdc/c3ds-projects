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
import java.util.Map;

import rals.code.CodeGenFeatureLevel;
import rals.code.MacroArgNameless;
import rals.expr.*;
import rals.lex.DefInfo;
import rals.types.RALType.AgentClassifier;
import rals.types.RALType.Opaque;

/**
 * Types (including the Scriptorium).
 * Where RALType's code represents the rules of the types, TypeSystem represents the management of those types.
 */
public class TypeSystem {
	public final Opaque gAny = new RALType.Any();
	public final Opaque gString = new Opaque(RALType.Major.String, "str");
	public final Opaque gInteger = new Opaque(RALType.Major.Value, "int");
	public final Opaque gBoolean = new Opaque(RALType.Major.Value, "bool", gInteger);
	public final Opaque gFloat = new Opaque(RALType.Major.Value, "float");
	public final Opaque gNull = new Opaque(RALType.Major.Agent, "null");
	public final Opaque gLambdaAny = new Opaque(RALType.Major.Lambda, "lambda");
	public final Opaque gImpossible = new Opaque(RALType.Major.Unknown, "impossible");
	public final Opaque gBytes = new Opaque(RALType.Major.ByteString, "bytes");
	public final RALType.AgentClassifier gAgent;
	public final RALType gAgentNullable;
	public final RALType gStringOrNumber;
	public final RALType gNumber;

	/**
	 * All agent types by classifier.
	 * Essentially the Scriptorium in a box.
	 */
	public final HashMap<Classifier, RALType.AgentClassifier> classifiers = new HashMap<>();

	/**
	 * These are all named types. To stop code from mucking around with this, it's private.
	 */
	private final HashMap<String, RALType> namedTypes = new HashMap<>();

	/**
	 * Named types definition points
	 */
	public final HashMap<String, DefInfo> namedTypesDefPoints = new HashMap<>();

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
	 * All lambdas by their signatures.
	 */
	public final HashMap<RALLambdaSignature, RALType.Lambda> lambdas = new HashMap<>();

	/**
	 * Named constants!
	 */
	public final HashMap<String, RALConstant> namedConstants = new HashMap<>();

	/**
	 * Named constants definition points
	 */
	public final HashMap<String, DefInfo> namedConstantsDefPoints = new HashMap<>();

	/**
	 * If these message numbers have special behaviour, add here.
	 */
	public final HashSet<Integer> messageHooks = new HashSet<Integer>();

	/**
	 * These script numbers are called on the wrong OWNR.
	 */
	public final HashMap<Integer, RALType> overrideOwnr = new HashMap<Integer, RALType>();

	/**
	 * Code generator feature level.
	 */
	public CodeGenFeatureLevel codeGenFeatureLevel = CodeGenFeatureLevel.c3;

	private int randomVariableNameNumber = 0;

	public TypeSystem() {
		declareTypedef("any", gAny, new DefInfo.Builtin("Represents literally any type. Therefore, cannot be stored without a cast due to the nature of CAOS's `set` commands."));
		declareTypedef("str", gString, new DefInfo.Builtin("Technically a list of bytes, meant to be text in the local system codepage."));
		declareTypedef("int", gInteger, new DefInfo.Builtin("A subset of `num` -- integers are 32-bit whole numbers from -2147483648 to 2147483647 inclusive.\nNote that involving floats in a calculation tends to make that number a float."));
		declareTypedef("bool", gBoolean, new DefInfo.Builtin("Can be either the `true` or `false` constant."));
		declareTypedef("float", gFloat, new DefInfo.Builtin("A floating-point number such as 1.25, these have infinite range, and higher precision at low values, but lose precision at larger values."));
		declareTypedef("null", gNull, new DefInfo.Builtin("The null agent reference. RAL uses a separate type for this to help prevent mistakes."));
		declareTypedef("bytes", gBytes, new DefInfo.Builtin("Actually a list of bytes. Cannot be stored or even have it's values dynamically controlled in any way -- only usable for inline purposes."));
		// setup Agent type
		gAgent = declareClass(new Classifier(0, 0, 0), "Agent", new DefInfo.Builtin("The root Agent type. All other Agent types are derived from this."));
		gAgentNullable = byNullable(gAgent);
		gNumber = byUnion(Arrays.asList(gFloat, gInteger));
		gStringOrNumber = byUnion(Arrays.asList(gNumber, gString));
		declareTypedef("num", gNumber, new DefInfo.Builtin("Number is the union of `int` and `float`, and therefore can be either.\nCAOS tends to refer to this as `v`, or when it accepts both types but cares particularly about which, `decimal`."));
	}

	public Iterable<Map.Entry<String, RALType>> getAllNamedTypes() {
		return namedTypes.entrySet();
	}

	public DefInfo getNamedTypeDefInfo(String key) {
		return namedTypesDefPoints.get(key);
	}

	/**
	 * Used for parser-generated variable names.
	 * These names are impossible to write as they start with : (not allowed in IDs).
	 */
	public String newParserVariableName() {
		return ":" + (randomVariableNameNumber++);
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
		rt = new RALType.AgentClassifier(this, cl, agParent);
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
	 * Gets a type by name, or null if not found.
	 * Use with care.
	 */
	public RALType byNameOpt(String name) {
		return namedTypes.get(name);
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
			return gImpossible;
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

	public RALType byLambda(Iterable<RALType> in, Iterable<MacroArgNameless> in2) {
		RALLambdaSignature rls = new RALLambdaSignature(in, in2);
		RALType.Lambda lambda = lambdas.get(rls);
		if (lambda == null) {
			LinkedList<RALType> lRets = new LinkedList<>();
			for (RALType rt : in)
				lRets.add(rt);
			LinkedList<MacroArgNameless> lArgs = new LinkedList<>();
			for (MacroArgNameless ma : in2)
				lArgs.add(ma);
			lambda = new RALType.Lambda(gLambdaAny, lRets.toArray(new RALType[0]), lArgs.toArray(new MacroArgNameless[0]));
			lambdas.put(rls, lambda);
		}
		return lambda;
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
	public RALType.AgentClassifier declareClass(Classifier cl, String name, DefInfo di) {
		RALType ort = namedTypes.get(name);
		RALType.AgentClassifier ag = byClassifier(cl);
		// already declared?
		if (ort == ag)
			return ag;
		// nevermind then
		checkConflictType(name);
		checkConflictInterface(name);
		namedTypes.put(name, ag);
		namedTypesDefPoints.put(name, di);
		namedInterfaces.put(name, ag.inherent);
		ag.typeName = name;
		return ag;
	}

	/**
	 * Declares an agent interface.
	 */
	public RALType.Agent declareInterface(String name, DefInfo di) {
		RALType ort = namedTypes.get(name);
		// already declared?
		if (ort != null)
			if (ort instanceof RALType.Agent)
				return (RALType.Agent) ort;
		// nevermind then
		checkConflictType(name);
		checkConflictInterface(name);
		RALType.Agent ag = new RALType.Agent(this, name);
		// Add Agent as a default parent, otherwise an interface can't be cast to Agent
		ag.addParent(gAgent);
		namedTypes.put(name, ag);
		namedTypesDefPoints.put(name, di);
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

	public void declareConst(String name, DefInfo di, RALConstant cst) {
		if (namedConstants.containsKey(name)) {
			DefInfo sp2 = namedConstantsDefPoints.get(name);
			if (sp2 != null)
				throw new RuntimeException("Constant conflict: " + name + " @ " + di.describePosition() + ", last definition " + sp2.describePosition());
			throw new RuntimeException("Constant conflict: " + name + " @ " + di.describePosition());
		}
		namedConstants.put(name, cst);
		namedConstantsDefPoints.put(name, di);
	}

	public AgentClassifier tryGetAsClassifier(String text) {
		RALType maybeClassifier = namedTypes.get(text);
		if (maybeClassifier instanceof RALType.AgentClassifier)
			return (RALType.AgentClassifier) maybeClassifier;
		return null;
	}

	public void declareTypedef(String name, RALType parseType, DefInfo di) {
		RALType existing = namedTypes.get(name);
		if (existing != null) {
			if (existing != parseType)
				throw new RuntimeException("Can't redeclare type " + name);
		} else {
			namedTypes.put(name, parseType);
			namedTypesDefPoints.put(name, di);
		}
	}
}
