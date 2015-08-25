package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;

public class TVPool {
	private final Map<TypeVar, Type> already = new HashMap<TypeVar, Type>();
	private char next = 65; // 'A'
	
	public Type get(TypeVar var) {
		if (already.containsKey(var))
			return already.get(var);
		Type nv = Type.polyvar(var.from != null ? var.from.posn : null, new String(new char[] { next++ }));
		already.put(var, nv);
		return nv;
	}

}
