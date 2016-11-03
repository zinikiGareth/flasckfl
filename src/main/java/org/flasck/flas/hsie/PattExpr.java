package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.exceptions.UtilException;

/** A PattExpr object holds the possible results of switching on a variable
 * 
 * For each pattern (var, const, type, constructor), a particular equation will be selected.
 *
 * <p>
 * &copy; 2016 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class PattExpr implements Iterable<Entry<Object, Integer>> {
	private final Map<Object, Integer> mapping = new HashMap<Object, Integer>();

	public InputPosition firstLocation() {
		InputPosition ret = null;
		for (Object o : mapping.keySet()) {
			InputPosition p = ((Locatable)o).location();
			if (ret == null)
				ret = p;
			else
				ret = ret.lesserOf(p);
		}
		if (ret == null)
			throw new UtilException("Cannot return null");
		return ret;
	}

	public void associate(Object patt, int expr) {
		mapping.put(patt, expr);
	}
	
	public PattExpr duplicate(Set<Integer> possibles) {
		PattExpr ret = new PattExpr();
		for (Entry<Object, Integer> m : mapping.entrySet()) {
//			System.out.println("Consider " + m.getKey() + " as " + m.getValue());
			if (possibles == null || possibles.contains(m.getValue()))
				ret.mapping.put(m.getKey(), m.getValue());
		}
		if (ret.mapping.isEmpty())
			return null;
		return ret;
	}
	
	public int singleExpr(Set<Integer> onlyCases) {
		Integer ret = null;
		for (Integer e : mapping.values())
			if (onlyCases != null && !onlyCases.contains(e))
				continue;
			else if (ret != null && !ret.equals(e))
				throw new HSIEException(null, "There is more than one remaining expression " + ret + " and " + e);
			else
				ret = e;
		if (ret == null)
			throw new HSIEException(null, "Cannot handle single expr with no expressions");
		return ret;
	}
	
	@Override
	public Iterator<Entry<Object, Integer>> iterator() {
		return mapping.entrySet().iterator();
	}

	public void dump() {
		for (Entry<Object, Integer> e : mapping.entrySet()) {
			System.out.println("  " + e.getKey() + " -> " + e.getValue());
		}
	}
	
	public String keyString() {
		return mapping.keySet().toString();
	}
	
	@Override
	public String toString() {
		return mapping.toString();
	}
}
