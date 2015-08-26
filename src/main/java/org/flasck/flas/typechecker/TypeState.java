package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.errors.ErrorResult;

public class TypeState {
	TypeEnvironment gamma;
	final TypeVariableMappings phi;
	final Map<String, Object> localKnowledge;

	public TypeState(ErrorResult errors) {
		gamma = new TypeEnvironment();
		phi = new TypeVariableMappings(errors);
		localKnowledge = new HashMap<String, Object>();
	}

}
