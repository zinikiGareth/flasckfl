package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;

public class PhiSolution {
	private final Map<TypeVar, Object> phi = new HashMap<TypeVar, Object>();
	
	public Object meaning(TypeVar tv) {
		if (phi.containsKey(tv))
			return phi.get(tv);
		else
			return tv;
	}

	/*
	public Object subst(TypeVar tv, Object in) {
		if (!phi.containsKey(tv))
			return in;
		
		return doSubst(tv, phi.get(tv), in);
	}

	private Object doSubst(TypeVar tv, Object typeExpr, Object in) {
		return null;
	}
	*/
}
