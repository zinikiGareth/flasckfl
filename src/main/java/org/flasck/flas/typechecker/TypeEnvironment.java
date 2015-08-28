package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class TypeEnvironment {
	private final Map<Var, TypeScheme> mapping = new HashMap<Var, TypeScheme>();
	
	public TypeEnvironment bind(Var var, TypeScheme tv) {
		TypeEnvironment ret = new TypeEnvironment();
		ret.mapping.putAll(mapping);
		ret.mapping.put(var, tv);
		if (mapping.containsKey(var))
			throw new UtilException("This is overwriting " + var + " which may or may not be OK");
		return ret;
	}

	public TypeScheme valueOf(CreationOfVar var) {
		if (!mapping.containsKey(var.var))
			throw new UtilException("Unbound variable " + var.var);
		return mapping.get(var.var);
	}

	public TypeEnvironment subst(TypeVariableMappings phi) {
		TypeEnvironment ret = new TypeEnvironment();
		for (Entry<Var, TypeScheme> x : mapping.entrySet()) {
			ret.bind(x.getKey(), x.getValue().subst(phi));
		}
		return ret;
	}
	@Override
	public String toString() {
		return "Gamma[" + mapping + "]";
	}
}
