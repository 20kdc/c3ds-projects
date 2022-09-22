/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

import java.lang.reflect.Field;
import java.util.LinkedList;

/**
 * All the config for everything everywhere.
 */
public abstract class BaseConfig {
	public abstract void visit(IConfigProvider icp);

	public static class Group extends BaseConfig {
		@Override
		public void visit(IConfigProvider icp) {
			try {
				for (Field f : getClass().getFields()) {
					Object obj = f.get(this);
					if (obj instanceof BaseConfig)
						((BaseConfig) obj).visit(icp);
				}
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static abstract class Opt extends BaseConfig {
		public final String key;
		public String description;
		public final Object defValue;
		private LinkedList<Runnable> observers = new LinkedList<Runnable>();

		public Opt(String k, Object d) {
			key = k;
			defValue = d;
		}

		@Override
		public void visit(IConfigProvider icp) {
			String str = icp.configVisit(key, valueToString(), description);
			if (str != null) {
				try {
					setValueFromString(str);
				} catch (Exception ex) {
					// whoopsie, and no log either probably
					ex.printStackTrace();
				}
			}
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

	public static abstract class OptGeneric<V> extends Opt {
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

	public static class Int extends OptGeneric<Integer> {
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

	public static class Bool extends OptGeneric<Boolean> {
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

	public static class Str extends OptGeneric<String> {
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

	public static class Emu<T extends Enum<T>> extends OptGeneric<T> {
		public Emu(String k, T defValue) {
			super(k, defValue);
		}

		public Emu<T> describe(String info) {
			super.describe(info);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected T valueFromString(String str) {
			return (T) Enum.valueOf((Class<T>) defValue.getClass(), str);
		}

		@Override
		public String valueToString() {
			return getValue().name();
		}
	}
}
