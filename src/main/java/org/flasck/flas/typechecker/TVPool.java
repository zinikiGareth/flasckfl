package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;

public class TVPool {
	private final Map<TypeVar, Type> already = new HashMap<TypeVar, Type>();
	private final Map<TypeVar, Integer> varCounts;
	private final Type any;
	private char next = 65; // 'A'
	
	public TVPool(Map<TypeVar, Integer> varCounts, Type any) {
		this.varCounts = varCounts;
		this.any = any;
	}

	public Type get(TypeVar var) {
		if (varCounts.get(var) == 1)
			return any;
		if (already.containsKey(var))
			return already.get(var);
		Type nv = Type.polyvar(var.from != null ? var.from.posn : null, new String(new char[] { next++ }));
		already.put(var, nv);
		return nv;
	}

}
