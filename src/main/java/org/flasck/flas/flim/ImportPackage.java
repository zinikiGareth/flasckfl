package org.flasck.flas.flim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ImportPackage implements Iterable<Map.Entry<String, Object>> {
	private final String pkgName;
	private final HashMap<String, Object> map;

	public ImportPackage(String pkgName) {
		this.pkgName = pkgName;
		this.map = new HashMap<String, Object>();
	}

	public Map.Entry<String, Object> getEntry(String name) {
		for (Entry<String, Object> r : map.entrySet()) {
			if (r.getKey().equals(name))
				return r;
		}
		return null;
	}

	public void define(String key, Object value) {
		String pn = pkgName == null ? key : pkgName+"."+key;
		map.put(pn, value);
	}

	public Object get(String name) {
		return map.get(name);
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return map.entrySet().iterator();
	}
}
