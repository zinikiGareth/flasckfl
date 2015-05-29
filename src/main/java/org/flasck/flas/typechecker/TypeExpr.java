package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.UtilException;

public class TypeExpr {
	public final String type;
	public final List<Object> args;

	public TypeExpr(String type, List<Object> args) {
		this.type = type;
		if (args == null)
			this.args = new ArrayList<Object>();
		else
			this.args = args;
	}

	public TypeExpr(String type, Object... exprs) {
		this.type = type;
		this.args = new ArrayList<Object>();
		for (Object o : exprs)
			args.add(o);
	}

	// Test if two type expressions are exactly the same, to the very comma
	// Type variables would need to be THE SAME variables to pass this test - they are not all created equal
	public boolean identicalTo(TypeExpr add) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean containsVar(TypeVar tv) {
		boolean ret = false;
		for (Object o : args) {
			if (o instanceof TypeVar && tv.equals(o))
				return true;
			else if (o instanceof TypeExpr)
				ret |= ((TypeExpr) o).containsVar(tv);
		}
		return ret;
	}

	// Convert to a "standard" type, not a working one
	public Type asType(TypeChecker tc) {
		try {
			return convertToType(tc, new TVPool());
		} catch (TypeUnion.FailException ex) {
			return null;
		}
	}

	protected Type convertToType(TypeChecker tc, TVPool pool) {
		if (this.type.equals("()")) { // tuple
			return Type.tuple(convertArgs(tc, pool, args));
		} else if (this.type.equals("->")) { // function
			List<Type> args = new ArrayList<Type>();
			List<Object> stack = new ArrayList<Object>();
			Object te = this;
			while (te instanceof TypeExpr && ((TypeExpr)te).type.equals("->")) {
				stack.add(((TypeExpr)te).args.get(0)); // lhs
				te = ((TypeExpr)te).args.get(1);
			}
			stack.add(te);
			for (Object o : stack)
				args.add(convertOne(tc, pool, o));
			return Type.function(convertArgs(tc, pool, stack));
		} else { // standard, possibly polymorphic
			return Type.simple(type, convertArgs(tc, pool, args));
		}
	}

	protected List<Type> convertArgs(TypeChecker tc, TVPool pool, Iterable<Object> from) {
		List<Type> args = new ArrayList<Type>();
		for (Object o : from)
			args.add(convertOne(tc, pool, o));
		return args;
	}

	protected Type convertOne(TypeChecker tc, TVPool pool, Object o) {
		if (o instanceof TypeVar)
			return pool.get((TypeVar)o);
		else if (o instanceof TypeExpr)
			return ((TypeExpr)o).asType(tc);
		else if (o instanceof TypeUnion) {
			TypeUnion tu = (TypeUnion)o;
			for (TypeDefn d : tc.types.values()) {
				Set<Map.Entry<TypeReference, TypeExpr>> match = tu.matchesExactly(d);
				if (match != null) {
					System.out.println("====");
					Map<String, Object> checkBindings = new LinkedHashMap<String, Object>();
					for (Entry<TypeReference, TypeExpr> x : match) {
						System.out.println(x);
						TypeReference want = x.getKey();
						Iterator<Object> have = x.getValue().args.iterator();
						for (Object v : want.args) {
							// TODO: this is a deprecated case because we don't handle SWITCH properly
							if (!have.hasNext())
								continue;
							TypeReference vr = (TypeReference) v;
							Object hv = have.next();
							System.out.println(vr.name);
							if (checkBindings.containsKey(vr.name)) {
								if (!hv.equals(checkBindings.get(vr.name))) {
									tc.errors.message((Tokenizable)null, "inconsistent parameters to " + want.name);
									throw new TypeUnion.FailException();
								}
								System.out.println("Compare " + hv + " and " + checkBindings.get(vr.name));
							} else
								checkBindings.put(vr.name, hv);
						}
					}
					System.out.println("====");
					return Type.simple(d.defining.name, convertArgs(tc, pool, checkBindings.values()));
				}
			}
			tc.errors.message((Tokenizable)null, "The union of " + tu + " is not a valid type");
			throw new TypeUnion.FailException();
		} else
			throw new UtilException("Cannot convert " + (o == null?"null":o.getClass()));
	}

	@Override
	public String toString() {
		return type+(args.isEmpty()?"":"("+args+")");
	}
}
