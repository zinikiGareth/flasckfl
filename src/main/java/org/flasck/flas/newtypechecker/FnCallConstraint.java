package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

public class FnCallConstraint implements Constraint {
	private final List<TypeInfo> argtypes = new ArrayList<TypeInfo>();

	public FnCallConstraint(List<TypeInfo> argtypes, TypeVar typeVar) {
		this.argtypes.addAll(argtypes);
		this.argtypes.add(typeVar);
	}

	public FnCallConstraint(List<TypeInfo> signature) {
		this.argtypes.addAll(signature);
	}

	
	@Override
	public TypeInfo typeInfo() {
		return new TypeFunc(argtypes);
	}

	@Override
	public String toString() {
		String sep = "";
		StringBuilder ret = new StringBuilder();
		for (TypeInfo ti : argtypes) {
			ret.append(sep);
			ret.append(ti);
			sep = "->";
		}
		return ret.toString();
	}
}
