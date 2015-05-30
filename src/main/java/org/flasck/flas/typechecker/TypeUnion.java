package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

public class TypeUnion implements Iterable<TypeExpr> {
	@SuppressWarnings("serial")
	public static class FailException extends RuntimeException {

	}

	final List<TypeExpr> union = new ArrayList<TypeExpr>();
	
	public void add(Object toAdd) {
		if (toAdd == null)
			throw new UtilException("Deal with nulls; don't give them to me");
		if (toAdd instanceof TypeExpr) {
			TypeExpr addTe = (TypeExpr) toAdd;
			for (TypeExpr te : union) {
				if (te.identicalTo(addTe))
					return;
			}
			union.add(addTe);
		} else if (toAdd instanceof TypeUnion) {
			for (TypeExpr te : ((TypeUnion)toAdd).union)
				add(te);
		} else
			throw new UtilException("Can't add a " + toAdd.getClass() + " to a union type");
	}

	public Set<Map.Entry<TypeReference, TypeExpr>> matchesExactly(TypeDefn d) {
		Map<TypeReference, TypeExpr> ret = new HashMap<TypeReference, TypeExpr>();
		// Build a list of all the constructors which are used in the union
		// If the actual type is used, that's fine, but don't include it
		Map<String, TypeExpr> ctors = new TreeMap<String, TypeExpr>(new StringComparator());
		boolean haveDefn = false;
		for (TypeExpr te : union)
			if (te.type.equals(d.defining.name)) {
				ret.put(d.defining, te);
				haveDefn = true;
				continue;
			} else
				ctors.put(te.type, te);
		// Now go through all the official case defns; if any of them is not in our list, return false
		// Otherwise, check that ALL of them are there by removing them and asserting that the list is then empty
		for (TypeReference x : d.cases) {
			if (!ctors.containsKey(x.name))
				if (!haveDefn)
					return null;
				else
					continue;
			ret.put(x, ctors.remove(x.name));
		}
		return ret.entrySet();
	}

	@Override
	public Iterator<TypeExpr> iterator() {
		return union.iterator();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		String sep = "";
		for (TypeExpr te : union) {
			sb.append(sep);
			sb.append(te);
			sep = ",";
		}
		sb.append("}");
		return sb.toString();
	}
}
