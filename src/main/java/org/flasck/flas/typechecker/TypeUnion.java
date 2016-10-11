package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
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

	public Set<Entry<Type, TypeExpr>> matchesEnough(RWUnionTypeDefn d) {
		Map<Type, TypeExpr> used = new HashMap<Type, TypeExpr>();
		Map<String, TypeExpr> unused = new TreeMap<String, TypeExpr>(new StringComparator());
		if (!buildCompositeLists(d, used, unused))
			return null;
		return used.entrySet();
	}

	public Set<Map.Entry<Type, TypeExpr>> matchesExactly(RWUnionTypeDefn d) {
		Map<Type, TypeExpr> used = new HashMap<Type, TypeExpr>();
		Map<String, TypeExpr> unused = new TreeMap<String, TypeExpr>(new StringComparator());
		if (!buildCompositeLists(d, used, unused))
			return null;
		if (!unused.isEmpty())
			return null;
		return used.entrySet();
	}

	// Build a list of all the constructors which are used in the union
	// If the actual type is used, that's fine, but don't include it
	// Also build a list of all the ones that aren't used
	private boolean buildCompositeLists(RWUnionTypeDefn d,  Map<Type, TypeExpr> used, Map<String, TypeExpr> unused) {
		boolean haveDefn = false;
		for (TypeExpr te : union)
			if (te.type.name().equals(d.name())) {
				used.put(d, te);
				haveDefn = true;
				continue;
			} else
				unused.put(te.type.name(), te);
		// Now go through all the official case defns; if any of them is not in our list, return false
		// Otherwise, check that ALL of them are there by removing them and asserting that the list is then empty
		for (Type x : d.cases) {
			if (!unused.containsKey(x.name()))
				if (!haveDefn)
					return false;
				else
					continue;
			used.put(x, unused.remove(x.name()));
		}
		return true;
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

	public boolean containsAny() {
		for (TypeExpr x : this.union)
			if (x.type.equals("Any") && x.args.isEmpty())
				return true; 
		return false;
	}
}
