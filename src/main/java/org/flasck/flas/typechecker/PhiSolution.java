package org.flasck.flas.typechecker;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.zinutils.exceptions.UtilException;

public class PhiSolution {
	private final Map<TypeVar, Object> phi = new HashMap<TypeVar, Object>();
	private final ErrorResult errors;
	
	public PhiSolution(ErrorResult errors) {
		this.errors = errors;
	}
	
	public Object meaning(TypeVar tv) {
		if (phi.containsKey(tv))
			return phi.get(tv);
		else
			return tv;
	}

	public void bind(TypeVar tv, Object val) {
		phi.put(tv, val);
	}

	public Object subst(Object in) {
		if (in instanceof TypeVar) {
			TypeVar tv = (TypeVar) in;
			if (phi.containsKey(tv))
				return phi.get(tv);
			else
				return tv;
		} else {
			TypeExpr te = (TypeExpr) in;
			List<Object> mapped = new ArrayList<Object>();
			for (Object o : te.args)
				mapped.add(subst(o));
			return new TypeExpr(te.type, mapped);
		}
	}

	public void unify(Object t1, Object t2) {
		System.out.println("Unify " + t1 + " and " +t2);
		if (t1 instanceof TypeVar && t2 instanceof TypeVar) {
			// case 1a: call extend
			extend((TypeVar)t1, subst((TypeVar) t2));
		} else if (t1 instanceof TypeVar || t2 instanceof TypeVar) {
			// case 1b & 2: one is a variable and the other isn't (and some 1a)
			TypeVar v;
			TypeExpr te;
			if (t1 instanceof TypeVar) {
				v = (TypeVar) t1;
				te = (TypeExpr) t2;
			} else {
				v = (TypeVar) t2;
				te = (TypeExpr) t1;
			}
			Object phitvn = meaning(v);
			Object phit = subst(te);
			if (phitvn instanceof TypeVar)
				extend((TypeVar)phitvn, phit); // back to case 1a after substitution did nothing
			else
				// We need to apply <tt>subst</tt> on the type expression, <tt>meaning</tt> on the variable, and then unify the two results.
				unify(meaning(v), subst(te));
		} else if (t1 instanceof TypeExpr && t2 instanceof TypeExpr) {
			// case 3 : check for same constructors and then unify the lists
			TypeExpr te1 = (TypeExpr) t1;
			TypeExpr te2 = (TypeExpr) t2;
			if (!te1.type.equals(te2.type)) {
				// we probably want a clearer message than this
				errors.message((Block)null, "Cannot unify " + te1.type + " and " + te2.type);
				return;
			}
			unifyl(te1.args, te2.args);
		} else
			throw new UtilException("I claim all the cases should be covered");
	}

	private void extend(TypeVar tv, Object te) {
		if (te instanceof TypeVar && tv.equals(te))
			return; // we known that tv == tv
		else if (te instanceof TypeExpr && ((TypeExpr)te).containsVar(tv))
			errors.message((Block)null, "This is a circularity");
		else
			bind(tv, te);
	}
	private void unifyl(List<Object> l1, List<Object> l2) {
		if (l1.size() != l2.size())
			throw new UtilException("This really shouldn't be possible");
		for (int i=0;i<l1.size();i++)
			unify(l1.get(i), l2.get(i));
	}
}
