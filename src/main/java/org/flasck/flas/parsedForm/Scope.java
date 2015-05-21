package org.flasck.flas.parsedForm;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.zinutils.exceptions.UtilException;

public class Scope implements Iterable<Entry<String, Object>> {
	private final Map<String, Object> defns = new TreeMap<String, Object>();

	public boolean contains(String name) {
		return defns.containsKey(name);
	}

	public void define(String name, Object defn) {
		if (defns.containsKey(name))
			throw new UtilException("Cannot provide multiple definitions of " + name);
		defns.put(name,  defn);
	}

	public int size() {
		return defns.size();
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return defns.entrySet().iterator();
	}

}
