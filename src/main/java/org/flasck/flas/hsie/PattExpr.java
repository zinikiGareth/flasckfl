package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.zinutils.exceptions.UtilException;

public class PattExpr implements Iterable<Entry<Object, SubstExpr>> {
	private final Map<Object, SubstExpr> mapping = new HashMap<Object, SubstExpr>();

	public void associate(Object patt, SubstExpr expr) {
		mapping.put(patt, expr);
	}
	
	public PattExpr duplicate(Set<SubstExpr> possibles) {
		PattExpr ret = new PattExpr();
		for (Entry<Object, SubstExpr> m : mapping.entrySet()) {
			System.out.println("Consider " + m.getKey() + " as " + m.getValue());
			if (possibles.contains(m.getValue()))
				ret.mapping.put(m.getKey(), m.getValue());
		}
		if (ret.mapping.isEmpty())
			return null;
		return ret;
	}
	
	public SubstExpr singleExpr() {
		if (mapping.size() > 1) {
			System.out.println("---error");
			dump();
			throw new UtilException("There is more than one remaining expression");
		}
		if (mapping.isEmpty())
			return null;
		return mapping.entrySet().iterator().next().getValue();
	}
	
	@Override
	public Iterator<Entry<Object, SubstExpr>> iterator() {
		return mapping.entrySet().iterator();
	}

	public void dump() {
		for (Entry<Object, SubstExpr> e : mapping.entrySet()) {
			System.out.println("  " + e.getKey() + " -> " + e.getValue());
		}
	}
	
	@Override
	public String toString() {
		return mapping.toString();
	}
}
