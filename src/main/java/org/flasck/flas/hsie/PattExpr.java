package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PattExpr {
	private final Map<Object, SubstExpr> mapping = new HashMap<Object, SubstExpr>();

	public void associate(Object patt, SubstExpr expr) {
		mapping.put(patt, expr);
	}

	public void dump() {
		for (Entry<Object, SubstExpr> e : mapping.entrySet()) {
			System.out.println("  " + e.getKey() + " -> " + e.getValue());
		}
	}
}
