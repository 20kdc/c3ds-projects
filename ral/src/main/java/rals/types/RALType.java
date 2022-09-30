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
	 * Can this be implicitly cast to the given type?
	 */
	public final boolean canImplicitlyCast(RALType other) {
		if (other == this)
			return true;
		if (other instanceof Any)
			return true;
		if (other instanceof Union) {
			if (this instanceof Union) {
				// Does the union include all our types?
				if (((Union) other).contents.containsAll(((Union) this).contents))
					return true;
			} else {
				// Does the union include us?
				if (((Union) other).contains(this))
					return true;
			}
		}
		// Check parent types
		if (this instanceof Agent) {
			Agent tAgent = (Agent) this;
			for (Agent a : tAgent.parents)
				if (a.canImplicitlyCast(other))
					return true;
		}
		return false;
	}

	public final void implicitlyCastOrThrow(RALType type) {
		if (!canImplicitlyCast(type))
			throw new RuntimeException("Cannot cast " + this + " to " + type);
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
	 * Lookup a message ID by name.
	 */
	public final Integer lookupMessageID(String name) {
		for (AgentInterface ai : interfaces) {
			Integer a = ai.messages.get(name);
			if (a != null)
				return a;
		}
		return null;
	}

	/**
	 * Lookup a message ID by name.
	 */
	public final String lookupMessageName(int id) {
		for (AgentInterface ai : interfaces) {
			String a = ai.messagesInv.get(id);
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

		public Opaque(Major mj, String nameHint) {
			super(mj);
			name = nameHint;
			regenInterfaces();
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		protected AgentInterface[] genInterfaces() {
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
				res.append(rt.toString());
			}
			return res.toString();
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
		public final AgentInterface inherent = new AgentInterface();

		/**
		 * Type name. Is changed by TypeSystem when a class name is declared.
		 */
		public String typeName;

		/**
		 * Additional interfaces that this agent implements.
		 * (You know, *in addition* to the ones inherited from parents.)
		 */
		private HashSet<Agent> parents = new HashSet<>();

		public Agent(String tn) {
			super(Major.Agent);
			typeName = tn;
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

		@Override
		public String toString() {
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

		public AgentClassifier(Classifier c, AgentClassifier p) {
			super(c.toString());
			classifier = c;
			classifierParentAsAgent = p;
			if (p != null)
				addParent(p);
		}
	}
}
