/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

import java.util.LinkedList;

/**
 * All the config for everything everywhere.
 */
public class BaseConfig {
	public final LinkedList<Opt> allOptions = new LinkedList<>();

	public void readInFrom(IConfigProvider icp) {
		for (Opt o : allOptions) {
			String str = icp.configVisit(o.key, o.valueToString(), o.description);
			if (str != null) {
				try {
					o.setValueFromString(str);
				} catch (Exception ex) {
					// whoopsie, and no log either probably
					ex.printStackTrace();
				}
			}
		}
	}

	public abstract class Opt {
		public final String key;
		public String description;
		public final Object defValue;
		private LinkedList<Runnable> observers = new LinkedList<Runnable>();

		public Opt(String k, Object d) {
			key = k;
			defValue = d;
			allOptions.add(this);
		}

		public Opt describe(String info) {
			description = info;
			return this;
		}

		public abstract Object getValue();
		public abstract void setValueFromString(String str);
		public abstract String valueToString();
		protected final void pokeObservers() {
			synchronized (observers) {
				for (Runnable c : observers)
					c.run();
			}
		}
		public final void addObserver(Runnable ob) {
			synchronized (observers) {
				observers.add(ob);
			}
		}
		public final void rmObserver(Runnable ob) {
			synchronized (observers) {
				observers.remove(ob);
			}
		}
		protected abstract Object valueFromString(String str);
	}
	public abstract class OptGeneric<V> extends Opt {
		private V value;
		public OptGeneric(String k, V d) {
			super(k, d);
			value = d;
		}

		public OptGeneric<V> describe(String info) {
			super.describe(info);
			return this;
		}

		public final V getValue() {
			return value;
		}

		@Override
		public void setValueFromString(String str) {
			value = valueFromString(str);
			pokeObservers();
		}

		protected abstract V valueFromString(String str);
	}

	public class Int extends OptGeneric<Integer> {
		public Int(String k, int defValue) {
			super(k, defValue);
		}

		public Int describe(String info) {
			super.describe(info);
			return this;
		}

		@Override
		protected Integer valueFromString(String str) {
			return Integer.valueOf(str);
		}

		@Override
		public String valueToString() {
			return getValue().toString();
		}
	}

	public class Bool extends OptGeneric<Boolean> {
		public Bool(String k, boolean defValue) {
			super(k, defValue);
		}

		public Bool describe(String info) {
			super.describe(info);
			return this;
		}

		@Override
		protected Boolean valueFromString(String str) {
			return Boolean.valueOf(str);
		}

		@Override
		public String valueToString() {
			return getValue().toString();
		}
	}

	public class Str extends OptGeneric<String> {
		public Str(String k, String defValue) {
			super(k, defValue);
		}

		public Str describe(String info) {
			super.describe(info);
			return this;
		}

		@Override
		protected String valueFromString(String str) {
			return str;
		}

		@Override
		public String valueToString() {
			return getValue();
		}
	}
}
