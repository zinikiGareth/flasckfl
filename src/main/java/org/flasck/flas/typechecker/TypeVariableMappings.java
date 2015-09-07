package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

/** This class has a set of mappings for type variables that can be looked up.
 * In the original Hancock paper, he describes this as a "solution" (phi) to a set of 
 * "simultaneous equations" over the types in scope at a given time.  This level of
 * complexity only really emerges when you have mutual recursion, but the same
 * technique is used all the time.
 * <p>
 * Entries are created here especially during unification.  Basically, in the unification
 * phase (which is implemented as a method on this class), it is observed that two "types" must be the same and, if one of them is a variable,
 * it is said to take on the other expression.  It is then possible to replace that
 * variable everywhere with the expression.
 * <p>
 * I would like to name this class something more meaningful, like "type variable substitutions"
 * <p>
 * &copy; 2015 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class TypeVariableMappings {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	private final Map<TypeVar, Object> phi = new HashMap<TypeVar, Object>();
	private final ErrorResult errors;
	private final List<TypeUnion> needTypeResolution = new ArrayList<TypeUnion>();
	
	public TypeVariableMappings(ErrorResult errors) {
		this.errors = errors;
	}
	
	/** Define the meaning of a variable
	 * @param tv the variable to define
	 * @param val its new meaning
	 */
	public void bind(TypeVar tv, Object val) {
		phi.put(tv, val);
	}

	/** Extract the current meaning of a variable
	 * @param tv the variable to search for
	 * @return the type expression (if any) in the mapping; if none, return the variable unchanged
	 */
	public Object meaning(TypeVar tv) {
		if (phi.containsKey(tv))
			return phi.get(tv);
		else
			return tv;
	}

	/** Given an arbitrary type expression "in", replace _all_ of the variables
	 * it contains which are in our solution mapping with their most resolved
	 * expressions
	 * @param in an arbitrary type variable or expression
	 * @return a type variable or expression with all the variables in our solution replaced with their current meanings
	 */
	public Object subst(Object in) {
		if (in instanceof TypeVar) {
			TypeVar tv = (TypeVar) in;
			if (phi.containsKey(tv))
				return phi.get(tv);
			else
				return tv;
		} else if (in instanceof TypeExpr) {
			TypeExpr te = (TypeExpr) in;
			List<Object> mapped = new ArrayList<Object>();
			for (Object o : te.args)
				mapped.add(subst(o));
			return new TypeExpr(te.from, te.type, mapped);
		} else if (in instanceof TypeUnion) {
			TypeUnion ret = new TypeUnion();
			for (TypeExpr te : (TypeUnion)in)
				ret.add(subst(te));
			return ret;
		} else
			throw new UtilException("Cannot handle " + (in == null? "null" : in.getClass()));
	}

	/** Attempt a deep unification of two types
	 * @param t1 a type variable or expression
	 * @param t2 a type variable or expression
	 * @return a single type variable or expression which is "valid" for both of the input types
	 */
	public Object unify(Object t1, Object t2) {
		logger.info("unifying " + t1 + " and " +t2);
		if (t1 == null || t2 == null)
			return null;
		else if (t1 instanceof TypeVar && t2 instanceof TypeVar) {
			// case 1a: call extend
			if (!phi.containsKey(t1))
				return extend((TypeVar)t1, subst((TypeVar) t2));
			else if (!phi.containsKey(t2))
				return extend((TypeVar)t2, subst((TypeVar) t1));
			else // go around again, probably for a different case
				return unify(meaning((TypeVar) t1), meaning((TypeVar) t2));
		} else if (t1 instanceof TypeVar || t2 instanceof TypeVar) {
			// case 1b & 2: one is a variable and the other isn't (and some 1a)
			TypeVar v;
			Object te;
			if (t1 instanceof TypeVar) {
				v = (TypeVar) t1;
				te = t2;
			} else {
				v = (TypeVar) t2;
				te = t1;
			}
			Object phitvn = meaning(v);
			Object phit = subst(te);
			if (phitvn instanceof TypeVar)
				return extend((TypeVar)phitvn, phit); // back to case 1a after substitution did nothing
			else
				// We need to apply <tt>subst</tt> on the type expression, <tt>meaning</tt> on the variable, and then unify the two results.
				return unify(meaning(v), subst(te));
		} else if (t1 instanceof TypeExpr && t2 instanceof TypeExpr) {
			// case 3 : check for same constructors and then unify the lists
			TypeExpr te1 = (TypeExpr) t1;
			TypeExpr te2 = (TypeExpr) t2;
			if (te1.type.equals(te2.type)) {
				// TODO: in doing this, we want to replace each of the TEs args "Garnered From" with GarneredFrom(te, arg#) if it is a "type" rather than a position (i.e. for the type var)
				List<Object> args = unifyl(te1.args, te2.args);
				return new TypeExpr(te1.from, te1.type, args);
			}
			if (te1.type.name().equals("Any"))
				return te1;
			else if (te2.type.name().equals("Any"))
				return te2;
			if (!isCtorBased(te1) || !isCtorBased(te2)) {
				UnificationError ue = new UnificationError(te1, te2);
				errors.message(ue);
				return null;
			}

//			// this is just for debugging; we should catch actual unification errors later
//			if (!isListCtor(te1) || !isListCtor(te2)) {
//				System.out.println("First pass does not unify " + te1 + " and " + te2);
//			}
			TypeUnion unified = unionOf(te1, te2);
			for (Entry<TypeVar, Object> x : phi.entrySet()) {
				if (x.getValue() == te1) {
					x.setValue(unified);
				}
				if (x.getValue() == te2) {
					x.setValue(unified);
				}
			}
			needTypeResolution.add(unified);
			return unified;
		} else if (t1 instanceof TypeUnion) {
			((TypeUnion) t1).add(t2);
			return t1;
		} else if (t2 instanceof TypeUnion) {
			((TypeUnion) t2).add(t1);
			return t2;
		} else
			throw new UtilException("I claim all the cases should be covered but I could not handle the pair " + t1.getClass() + " and " + t2.getClass());
//		System.out.println("Unification done: " + this.phi);
	}

	private boolean isCtorBased(TypeExpr te) {
		Type ty  = te.type;
		while (ty.iam == WhatAmI.FUNCTION && ty.arity() == 0)
			ty = ty.arg(0);
		while (ty.iam == WhatAmI.INSTANCE)
			ty = ty.innerType();
		if (ty instanceof StructDefn || ty instanceof UnionTypeDefn)
			return true;
		return false;
	}

	private boolean isListCtor(TypeExpr expr) {
		return expr.type.name().equals("Nil") || expr.type.name().equals("Cons") || expr.type.name().equals("List");
	}

	private TypeUnion unionOf(TypeExpr te1, TypeExpr te2) {
		TypeUnion ret = new TypeUnion();
		ret.add(te1);
		ret.add(te2);
		return ret;
	}

	private Object extend(TypeVar tv, Object te) {
		if (te instanceof TypeVar && tv.equals(te)) {
			logger.info(tv + " and " + te + " are identical");
			return te; // we known that tv == tv
		} else if (te instanceof TypeExpr && ((TypeExpr)te).containsVar(tv)) {
			logger.info(te + " contains " + tv + " and is an error case");
			errors.message((Block)null, "This is a circularity");
			return null;
		} else {
			logger.info("defining " + tv + " to be " + te);
			bind(tv, te);
			return te;
		}
	}
	
	private List<Object> unifyl(List<Object> l1, List<Object> l2) {
		List<Object> ret = new ArrayList<Object>();
		if (l1.size() != l2.size()) {
			System.out.println("This really shouldn't be possible - inconsistent number of args to a constructor");
			return ret;
		}
		for (int i=0;i<l1.size();i++)
			ret.add(unify(l1.get(i), l2.get(i)));
		return ret;
	}

	// See PH p173
	public TypeVariableMappings exclude(List<TypeVar> varsToExclude) {
		TypeVariableMappings ret = new TypeVariableMappings(errors);
		for (Entry<TypeVar, Object> x : phi.entrySet()){
			if (!varsToExclude.contains(x.getKey()))
				ret.bind(x.getKey(), x.getValue());
		}
		return ret;
	}

	// Unlike in TypeExpr.convertOne, where we want to be very precise that there is an exact match,
	// here we just want to check that the two types are "compatible".
	public void validateUnionTypes(TypeChecker tc) {
//		System.out.println(this.needTypeResolution);
		checkNextUnion:
		for (TypeUnion tu : needTypeResolution) {
			if (tu.containsAny())
				continue;
			for (UnionTypeDefn d : tc.types.values()) {
				Set<Map.Entry<Type, TypeExpr>> match = tu.matchesEnough(d);
				if (match != null) {
//					System.out.println("====");
					Map<String, Object> checkBindings = new LinkedHashMap<String, Object>();
					for (Entry<Type, TypeExpr> x : match) {
//						System.out.println("Match: " + x);
						Type want = x.getKey();
						Iterator<Object> have = x.getValue().args.iterator();
						for (Type vr : want.polys()) {
							// TODO: this is a deprecated case because we don't handle SWITCH properly
							if (!have.hasNext())
								continue;
							if (vr.iam != WhatAmI.POLYVAR)
								throw new UtilException("should be a polyvar");
							Object hv = have.next();
							if (checkBindings.containsKey(vr.name())) {
								Object bound = checkBindings.get(vr.name());
								if (!hv.equals(bound)) {
									errors.message(want.location(), "inconsistent parameters to " + want.name() + ": " + hv + " and " + bound);
								}
							} else
								checkBindings.put(vr.name(), hv);
						}
					}
//					System.out.println("====");
					continue checkNextUnion;
				}
			}
			errors.message((Block)null, "The union of " + tu + " is not a valid type");
		}
	}
}
