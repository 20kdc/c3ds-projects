/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import rals.lex.DefInfo;
import rals.types.AgentInterface.OVar;

/**
 * Type in the RAL language.
 * This represents a set of CAOS types, but with more detail.
 * BEWARE! This file contains the typing rules for THE ENTIRE LANGUAGE!
 * This is on purpose so it's all in one clear place.
 */
public abstract class RALType {
	public final Major majorType;
	protected HashSet<RALType> influencesInterfacesOf = new HashSet<>();
	private AgentInterface[] interfaces;

	public RALType(Major mt) {
		majorType = mt;
	}

	/**
	 * Used by tooling
	 */
	public abstract String getFullDescription();

	/**
	 * Can this be implicitly cast to the given type?
	 */
	public final boolean canImplicitlyCast(RALType other) {
		if (other == this)
			return true;
		if (other instanceof Any)
			return true;
		if (this instanceof Union) {
			boolean ok = true;
			// If all of our subtypes fit in the target union then it's ok
			for (RALType opt : ((Union) this).contents) {
				if (!opt.canImplicitlyCast(other)) {
					ok = false;
					break;
				}
			}
			if (ok)
				return true;
		} else if (other instanceof Union) {
			Union uo = (Union) other;
			// If we fit in any subtype then it's okay
			// (for us to require multiple subtypes, that'd make us a union, see above)
			for (RALType otherOpt : uo.contents)
				if (canImplicitlyCast(otherOpt))
					return true;
		}
		// Check parent types
		if (this instanceof Opaque) {
			RALType pt = ((Opaque) this).parentType;
			if (pt != null && pt.canImplicitlyCast(other))
				return true;
		} else if (this instanceof Agent) {
			Agent tAgent = (Agent) this;
			for (Agent a : tAgent.parents)
				if (a.canImplicitlyCast(other))
					return true;
		}
		return false;
	}

	/**
	 * Assert this type can be implicitly cast to another (the supertype), then return this type (the subtype)
	 */
	public final RALType assertImpCast(RALType type) {
		if (!canImplicitlyCast(type))
			throw new RuntimeException("Cannot cast " + this + " to " + type);
		return this;
	}

	public final void implicitlyCastOrThrow(RALType type, Object src, Object dst) {
		if (!canImplicitlyCast(type))
			throw new RuntimeException("Cannot cast " + this + "(" + src + ") to " + type + "(" + dst + ")");
	}

	protected void regenInterfaces() {
		interfaces = genInterfaces();
		for (RALType rt : influencesInterfacesOf)
			rt.regenInterfaces();
	}

	/**
	 * Which agent interfaces can be accessed via this type?
	 * NOTE: An AgentInterface represents just one "view" of a type.
	 * The array is in first-wins order.
	 */
	protected abstract AgentInterface[] genInterfaces();

	/**
	 * Which agent interfaces can be accessed via this type?
	 * NOTE: An AgentInterface represents just one "view" of a type.
	 */
	public final AgentInterface[] getInterfaces() {
		return interfaces;
	}

	/**
	 * Lookup a message or script ID by name.
	 */
	public final Integer lookupMSID(String name, boolean asScript) {
		for (AgentInterface ai : interfaces) {
			Integer a = (asScript ? ai.scripts : ai.messages).get(name);
			if (a != null)
				return a;
		}
		return null;
	}

	/**
	 * Lookup a message or script name by ID.
	 */
	public final String lookupMSName(int id, boolean asScript) {
		for (AgentInterface ai : interfaces) {
			String a = (asScript ? ai.scriptsInv : ai.messagesInv).get(id);
			if (a != null)
				return a;
		}
		return null;
	}

	/**
	 * Lookup a field by name.
	 */
	public final OVar lookupField(String name) {
		for (AgentInterface ai : interfaces) {
			OVar a = ai.fields.get(name);
			if (a != null)
				return a;
		}
		return null;
	}

	public static enum Major {
		Unknown,
		Agent,
		String,
		Value;
	}

	/**
	 * Opaque types.
	 */
	public static class Opaque extends RALType {
		public final String name;
		public final RALType parentType;

		public Opaque(Major mj, String nameHint) {
			this(mj, nameHint, null);
		}
		public Opaque(Major mj, String nameHint, RALType p) {
			super(mj);
			name = nameHint;
			parentType = p;
			if (p != null)
				p.influencesInterfacesOf.add(this);
			regenInterfaces();
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public String getFullDescription() {
			return "opaque(" + name + ")";
		}

		@Override
		protected AgentInterface[] genInterfaces() {
			if (parentType != null)
				return parentType.genInterfaces();
			return new AgentInterface[0];
		}
	}

	/**
	 * Any (for instanceof)
	 */
	public static class Any extends Opaque {
		public Any() {
			super(Major.Unknown, "any");
		}
	}

	/**
	 * Union types.
	 */
	public static class Union extends RALType implements Iterable<RALType> {
		private HashSet<RALType> contents;

		private static Major getCommonMajorType(Iterable<RALType> x) {
			Major m = Major.Unknown;
			for (RALType rt : x) {
				if (rt.majorType == Major.Unknown)
					continue;
				if (m == Major.Unknown) {
					m = rt.majorType;
					continue;
				}
				// can be multiple
				if (m != rt.majorType)
					return Major.Unknown;
			}
			return m;
		}

		public Union(HashSet<RALType> c) {
			super(getCommonMajorType(c));
			contents = c;
			if (contents.size() <= 1)
				throw new RuntimeException("Can't have a union with only one member - stop creating your Unions directly!");
			for (RALType rt : c) {
				contents.add(rt);
				rt.influencesInterfacesOf.add(this);
			}
			regenInterfaces();
		}

		public boolean contains(RALType rt) {
			return contents.contains(rt);
		}

		@Override
		public Iterator<RALType> iterator() {
			return contents.iterator();
		}

		@Override
		public String toString() {
			StringBuilder res = new StringBuilder();
			boolean first = true;
			for (RALType rt : contents) {
				if (!first)
					res.append('|');
				first = false;
				res.append(rt);
			}
			return res.toString();
		}

		@Override
		public String getFullDescription() {
			return toString();
		}

		@Override
		protected AgentInterface[] genInterfaces() {
			HashSet<AgentInterface> all = new HashSet<>();
			for (RALType rt : contents) {
				if (rt.interfaces == null)
					throw new RuntimeException("attempted to gen interfaces when interfaces null on " + rt);
				for (AgentInterface ai : rt.interfaces)
					all.add(ai);
			}
			HashSet<AgentInterface> filtered = new HashSet<>();
			for (AgentInterface ai : all) {
				boolean ok = true;
				for (RALType rt : contents) {
					ok = false;
					for (AgentInterface ai2 : rt.interfaces)
						if (ai == ai2) {
							ok = true;
							break;
						}
					if (!ok)
						break;
				}
				if (ok)
					filtered.add(ai);
			}
			return filtered.toArray(new AgentInterface[0]);
		}
	}

	/**
	 * Generic "Agent" type.
	 * Used for interfaces.
	 */
	public static class Agent extends RALType {
		/**
		 * Inherent details about the agent.
		 */
		public final AgentInterface inherent;

		/**
		 * Type name. Is changed by TypeSystem when a class name is declared.
		 */
		public String typeName;

		/**
		 * Additional interfaces that this agent implements.
		 * (You know, *in addition* to the ones inherited from parents.)
		 */
		private HashSet<Agent> parents = new HashSet<>();

		/**
		 * Used for declaration stuff
		 */
		private final TypeSystem typeSystem;

		public Agent(TypeSystem ts, String tn) {
			super(Major.Agent);
			inherent = new AgentInterface(this);
			typeName = tn;
			typeSystem = ts;
			regenInterfaces();
		}

		/**
		 * Declares that this Agent implements/extends another.
		 */
		public void addParent(Agent impl) {
			parents.add(impl);
			impl.influencesInterfacesOf.add(this);
			regenInterfaces();
		}

		@Override
		protected AgentInterface[] genInterfaces() {
			LinkedList<AgentInterface> total = new LinkedList<>();
			total.add(inherent);
			for (Agent par : parents)
				for (AgentInterface ai : par.getInterfaces())
					if (!total.contains(ai))
						total.add(ai);
			return total.toArray(new AgentInterface[0]);
		}

		/**
		 * Declares a message or a script.
		 */
		public final void declareMS(String name, int id, boolean asScript) {
			boolean asMessage = !asScript;
			if (!typeSystem.messageHooks.contains(id)) {
				asMessage = true;
				asScript = true;
			}
			if (asMessage) {
				if (lookupMSID(name, false) != null)
					throw new RuntimeException("message " + name + ":" + name + " already declared");
				if (lookupMSName(id, false) != null)
					throw new RuntimeException("message " + name + " " + id + " already declared");
			}
			if (asScript) {
				if (lookupMSID(name, true) != null)
					throw new RuntimeException("script " + name + ":" + name + " already declared");
				if (lookupMSName(id, true) != null)
					throw new RuntimeException("script " + name + " " + id + " already declared");
			}
		
			if (asMessage) {
				inherent.messages.put(name, id);
				inherent.messagesInv.put(id, name);
			}
			if (asScript) {
				inherent.scripts.put(name, id);
				inherent.scriptsInv.put(id, name);
			}
		}

		/**
		 * Declares a field.
		 */
		public void declareField(String fieldName, RALType fieldType, DefInfo idi, int ovSlot) {
			if (lookupField(fieldName) != null)
				throw new RuntimeException("Field " + fieldName + " already exists in " + this);
			inherent.fields.put(fieldName, new OVar(ovSlot, fieldType, idi));
		}

		@Override
		public String toString() {
			return typeName;
		}

		@Override
		public String getFullDescription() {
			return typeName;
		}
	}

	/**
	 * A "real" Agent type with a specific Classifier in the Scriptorium.
	 */
	public static class AgentClassifier extends Agent {
		/**
		 * Classifier of this type.
		 * Null here indicates this is an interface type.
		 * Not-null here implies this is *the* Agent type for this classifier.
		 */
		public final Classifier classifier;

		/**
		 * Main parent of this type.
		 * Null here indicates this is 0 0 0.
		 */
		public final AgentClassifier classifierParentAsAgent;

		public AgentClassifier(TypeSystem ts, Classifier c, AgentClassifier p) {
			super(ts, c.toString());
			classifier = c;
			classifierParentAsAgent = p;
			if (p != null)
				addParent(p);
		}

		@Override
		public String getFullDescription() {
			return "classifier " + classifier.toString();
		}
	}
}
