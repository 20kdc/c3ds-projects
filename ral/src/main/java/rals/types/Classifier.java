/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

/**
 * A classifier.
 */
public final class Classifier implements Comparable<Classifier> {
	public final int family, genus, species;

	public Classifier(int f, int g, int s) {
		family = f;
		genus = g;
		species = s;
	}

	/**
	 * If this classifier is a valid wildcard for scripts.
	 */
	public boolean isScriptValid() {
		return (family == 0 && genus == 0 && species == 0) ||
				(family != 0 && genus == 0 && species == 0) ||
				(family != 0 && genus != 0 && species == 0) ||
				(family != 0 && genus != 0 && species != 0);
	}

	/**
	 * Generates the parent classifier, or null for none.
	 */
	public Classifier genScriptParent() {
		if (family != 0 && genus == 0 && species == 0)
			return new Classifier(0, 0, 0);
		if (family != 0 && genus != 0 && species == 0)
			return new Classifier(family, 0, 0);
		if (family != 0 && genus != 0 && species != 0)
			return new Classifier(family, genus, 0);
		return null;
	}

	/**
	 * Generates a following classifier (if possible, else will throw)
	 */
	public Classifier genScriptChild(int val) {
		if (family == 0 && genus == 0 && species == 0)
			return new Classifier(val, 0, 0);
		if (family != 0 && genus == 0 && species == 0)
			return new Classifier(family, val, 0);
		if (family != 0 && genus != 0 && species == 0)
			return new Classifier(family, genus, val);
		throw new RuntimeException("Cannot extend full/wrong classifier");
	}

	/**
	 * Returns true if any component is 0 (wildcard).
	 */
	public boolean isWildcard() {
		return family == 0 || genus == 0 || species == 0;
	}

	/**
	 * Returns true if this classifier contains the given classifier.
	 */
	public boolean contains(Classifier other) {
		if (family != 0 && other.family != family)
			return false;
		if (genus != 0 && other.genus != genus)
			return false;
		if (species != 0 && other.species != species)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + family + " " + genus + " " + species + "]";
	}

	/**
	 * Defined as the exact format "0 0 0" or "1 2 3" or w/e, i.e. will not change
	 */
	public String toCAOSString() {
		return family + " " + genus + " " + species;
	}

	@Override
	public int hashCode() {
		return family ^ genus ^ species;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Classifier) {
			if (((Classifier) obj).family != family)
				return false;
			if (((Classifier) obj).genus != genus)
				return false;
			if (((Classifier) obj).species != species)
				return false;
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(Classifier o) {
		if (family < o.family)
			return -1;
		if (family > o.family)
			return 1;
		if (genus < o.genus)
			return -1;
		if (genus > o.genus)
			return 1;
		if (species < o.species)
			return -1;
		if (species > o.species)
			return 1;
		return 0;
	}
}
