package org.flasck.flas.typechecker;

import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.zinutils.exceptions.UtilException;

public class PhiSolution {
	private final Map<TypeVar, Object> phi = new HashMap<TypeVar, Object>();
	
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
		}
		throw new UtilException("Missing cases");
	}
}
