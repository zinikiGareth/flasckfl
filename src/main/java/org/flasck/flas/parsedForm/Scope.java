package org.flasck.flas.parsedForm;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.zinutils.exceptions.UtilException;

public class Scope implements Iterable<Entry<String, Object>> {
	public class ScopeEntry implements Entry<String, Object> {
		private final String name;
		private Object defn;

		public ScopeEntry(String name, Object defn) {
			this.name = name;
			this.defn = defn;
		}

		@Override
		public String getKey() {
			return name;
		}

		@Override
		public Object getValue() {
			return defn;
		}

		@Override
		public Object setValue(Object value) {
			return defn;
		}
	}

	private final Scope outer;
	private final Map<String, Map.Entry<String, Object>> defns = new TreeMap<String, Map.Entry<String, Object>>();

	public Scope(Scope inside) {
		this.outer = inside;
	}
	
	public boolean contains(String key) {
		return defns.containsKey(key);
	}

	public void define(String key, String name, Object defn) {
		if (defns.containsKey(key))
			throw new UtilException("Cannot provide multiple definitions of " + name);
		System.out.println("Defining " + key + " with name " + name);
		defns.put(key, new ScopeEntry(name, defn));
	}

	public int size() {
		return defns.size();
	}

	public String resolve(String name) {
		if (name.contains("."))
			throw new UtilException("Cannot have '.' in name: " + name);
		if (defns.containsKey(name))
			return defns.get(name).getKey();
		if (outer != null)
			return outer.resolve(name);
		System.out.println("Could not resolve name " + name + " in " + defns.keySet());
		return "BUILTIN."+name;
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return defns.values().iterator();
	}

}
