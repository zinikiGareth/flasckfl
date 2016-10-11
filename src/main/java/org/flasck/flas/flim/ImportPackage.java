package org.flasck.flas.flim;

import java.util.HashMap;
import java.util.Map;

public class ImportPackage {
	private final String pkgName;
	private final HashMap<String, Object> map;

	public ImportPackage(String pkgName) {
		this.pkgName = pkgName;
		this.map = new HashMap<String, Object>();
	}

	public Map.Entry<String, Object> getEntry(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public void define(String key, Object value) {
		String pn = pkgName == null ? key : pkgName+"."+key;
		map.put(pn, value);
	}
}
