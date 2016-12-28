package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ScopeDefineException;
import org.zinutils.exceptions.UtilException;

public class Scope implements IScope, Iterable<Scope.ScopeEntry> {
	public static class ScopeEntry implements Entry<String, Object> {
		private final InputPosition location;
		private final String name;
		private Object defn;
		private final String key;

		public ScopeEntry(String key, String name, Object defn) {
			this.key = key;
			location = (defn == null)?null:((Locatable)defn).location();
			if (defn != null && this.location == null)
				throw new UtilException("null location se1");
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
		
		@Override
		public String toString() {
			return name + " => " + defn;
		}
	}

	private final List<ScopeEntry> defns = new ArrayList<ScopeEntry>();
	private final Set<String> index = new TreeSet<String>();
	public final NameOfThing scopeName;

	public Scope(NameOfThing name) {
		this.scopeName = name;
	}

	public static Scope topScope(String inPkg) {
		return new Scope(new PackageName(inPkg));
	}
	
	public boolean contains(String key) {
		return index.contains(key);
	}

	public String fullName(String name) {
		if (name.contains("."))
			return name;
		return scopeName.uniqueName() + "." + name;
	}

	@Override
	public ScopeEntry get(String key) {
		if (!contains(key))
			return null;
		for (ScopeEntry se : defns) {
			if (se.key.equals(key))
				return se;
		}
		return null;
	}

	@Override
	public void define(String key, Object defn) {
		if (key.contains("."))
			throw new ScopeDefineException("Cannot define an entry in a scope with a compound key: " + key);
		ScopeEntry ret = new ScopeEntry(key, scopeName.uniqueName()+"."+key, defn);
		defns.add(ret);
		index.add(key);
	}

	public int size() {
		return defns.size();
	}

	@Override
	public Iterator<ScopeEntry> iterator() {
		return defns.iterator();
	}

	public int caseName(String name) {
		int idx = name.lastIndexOf(".");
		String key = name.substring(idx+1); 
		int cs = 0;
		for (ScopeEntry se : this)
			if (se.key.equals(key))
				cs++;
		return cs;
	}

	@Override
	public String toString() {
		return defns.toString();
	}
}
