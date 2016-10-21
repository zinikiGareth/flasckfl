package org.flasck.flas.parsedForm;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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

	private final Map<String, ScopeEntry> defns = new LinkedHashMap<String, ScopeEntry>();
	public final ScopeEntry outerEntry;
	public final Object container;

	public Scope(ScopeEntry inside, Object container) {
		this.outerEntry = inside;
		this.container = container;
	}
	
	public boolean contains(String key) {
		return defns.containsKey(key);
	}

	public ScopeEntry define(String key, String name, Object defn) {
		if (key.contains("."))
			throw new ScopeDefineException("Cannot define an entry in a scope with a compound key: " + key);
		if (defns.containsKey(key))
			throw new ScopeDefineException("Cannot provide multiple definitions of " + name);
		ScopeEntry ret = new ScopeEntry(name, defn);
		defns.put(key, ret);
		return ret;
	}

	public int size() {
		return defns.size();
	}

	@Override
	public Iterator<ScopeEntry> iterator() {
		return defns.values().iterator();
	}

	public ScopeEntry getEntry(String key) {
		if (!defns.containsKey(key))
			return null;
		return defns.get(key);
	}

	@Override
	public String toString() {
		return defns.toString();
	}

	public String fullName(String name) {
		if (outerEntry != null)
			return outerEntry.name + "." + name;
		return name;
	}
}
