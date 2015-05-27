package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.vcode.hsieForm.Var;

public class TypeEnvironment {
	private final Map<Var, TypeScheme> mapping = new HashMap<Var, TypeScheme>();
	
	public TypeEnvironment bind(Var var, TypeScheme tv) {
		TypeEnvironment ret = new TypeEnvironment();
		ret.mapping.putAll(mapping);
		ret.mapping.put(var, tv);
		return ret;
	}

	@Override
	public String toString() {
		return "Gamma[" + mapping + "]";
	}
}
