package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.zinutils.exceptions.UtilException;

public class TypeUnion implements Iterable<TypeExpr> {
	private final List<TypeExpr> union = new ArrayList<TypeExpr>();
	
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
