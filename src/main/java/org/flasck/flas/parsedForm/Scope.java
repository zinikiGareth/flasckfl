package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ScopeDefineException;
import org.flasck.flas.rewriter.ResolutionException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

@SuppressWarnings("serial")
public class Scope implements Iterable<Entry<String, Scope.ScopeEntry>>, Serializable {
	public class ScopeEntry implements Entry<String, Object>, Serializable {
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

	public final Scope outer;
	private final Map<String, ScopeEntry> defns = new TreeMap<String, ScopeEntry>(new StringComparator());
	public final ScopeEntry outerEntry;
	public final Object container;

	public Scope(ScopeEntry inside, Object container) {
		this.outer = null;
		this.outerEntry = inside;
		this.container = container;
	}
	
	public boolean contains(String key) {
		return defns.containsKey(key);
	}

	public ScopeEntry define(String key, String name, Object defn) {
		if (key.contains(".") && !key.equals(".") && !(defn instanceof PackageDefn))
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

	public Object resolve(InputPosition location, String name) {
		if (name.contains("."))
			return name;
		if (defns.containsKey(name)) {
			throw new UtilException("Package or Scoped?");
//			return new PackageVar(defns.get(name));
		}
		try {
			if (outer != null)
				return outer.resolve(location, name);
		} catch (UtilException ex) { /* and rethrow ourselves */ }
		System.out.println("Could not resolve name " + name + " in " + defns.keySet());
		throw new ResolutionException(location, name);
	}
	
	public Set<String> keys() {
		return defns.keySet();
	}

	@Override
	public Iterator<Entry<String, ScopeEntry>> iterator() {
		return defns.entrySet().iterator();
	}

	public Object get(String key) {
		if (!defns.containsKey(key))
			return null;
		return defns.get(key).getValue();
	}

	public ScopeEntry getEntry(String key) {
		if (!defns.containsKey(key))
			return null;
		return (ScopeEntry) defns.get(key);
	}

	@Override
	public String toString() {
		return defns.toString();
	}

	public Object getResolved(String resolvedName) {
		if (outer != null)
			return outer.getResolved(resolvedName);
		if (resolvedName.contains("."))
			throw new UtilException("Not yet");
		return get(resolvedName);
	}

	public String fullName(String name) {
		if (outerEntry != null)
			return outerEntry.name + "." + name;
		return name;
	}
}
