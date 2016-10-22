package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ScopeDefineException;

public class Scope implements Iterable<Scope.ScopeEntry> {
	public class ScopeEntry implements Entry<String, Object> {
		private final InputPosition location;
		private final String name;
		private Object defn;

		public ScopeEntry(String name, Object defn) {
			location = (defn == null)?null:((Locatable)defn).location();
			if (defn != null && this.location == null)
				System.out.println("null location se1");
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

		public InputPosition location() {
			return location;
		}
		
		@Override
		public Object setValue(Object value) {
			this.defn = value;
			return defn;
		}
		
		public Scope scope() {
			return Scope.this;
		}
		
		@Override
		public String toString() {
			return name + " => " + defn;
		}
	}

	private final List<ScopeEntry> defns = new ArrayList<ScopeEntry>();
	private final Map<String, String> fullNames = new TreeMap<String, String>();
	public final Object container;

	public Scope(Object container) {
		this.container = container;
	}
	
	public boolean contains(String key) {
		return fullNames.containsKey(key);
	}

	public String fullName(String key) {
		return fullNames.get(key);
	}

	public ScopeEntry define(String key, String name, Object defn) {
		if (key.contains("."))
			throw new ScopeDefineException("Cannot define an entry in a scope with a compound key: " + key);
		ScopeEntry ret = new ScopeEntry(name, defn);
		defns.add(ret);
		fullNames.put(key, name);
		return ret;
	}

	public int size() {
		return defns.size();
	}

	@Override
	public Iterator<ScopeEntry> iterator() {
		return defns.iterator();
	}

	public String caseName(String name) {
		int cs = 0;
		for (ScopeEntry se : this)
			if (se.name.equals(name))
				cs++;
		return name +"_"+ cs;
	}

	@Override
	public String toString() {
		return defns.toString();
	}
}
