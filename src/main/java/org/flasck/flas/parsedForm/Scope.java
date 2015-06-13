package org.flasck.flas.parsedForm;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewriter.ResolutionException;
import org.zinutils.exceptions.UtilException;

public class Scope implements Iterable<Entry<String, Scope.ScopeEntry>> {
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
	private final Map<String, ScopeEntry> defns = new TreeMap<String, ScopeEntry>();
	public final ScopeEntry outerEntry;

	@Deprecated
	public Scope(Scope inside) {
		this.outer = inside;
		this.outerEntry = null;
	}
	
	public Scope(ScopeEntry inside) {
		this.outer = null;
		this.outerEntry = inside;
	}
	
	public boolean contains(String key) {
		return defns.containsKey(key);
	}

	public ScopeEntry define(String key, String name, Object defn) {
		if (key.contains(".") && !key.equals(".") && !(defn instanceof PackageDefn))
			throw new UtilException("Cannot define an entry in a scope with a compound key: " + key);
		if (defns.containsKey(key))
			throw new UtilException("Cannot provide multiple definitions of " + name);
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
		if (defns.containsKey(name))
			return new AbsoluteVar(defns.get(name));
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

	public AbsoluteVar fromRoot(String name) {
		if (outerEntry != null)
			return outerEntry.scope().fromRoot(name);
		else if (outer != null)
			return outer.fromRoot(name);
		int idx = name.indexOf('.');
		Scope scope = this;
		if (idx != -1) {
			PackageDefn pd = (PackageDefn) this.get(name.substring(0, idx));
			scope = pd.innerScope();
			name = name.substring(idx+1);
			if (name.indexOf('.') != -1)
				throw new UtilException("Can't do that");
		}
		return new AbsoluteVar(scope.getEntry(name));
	}
}
