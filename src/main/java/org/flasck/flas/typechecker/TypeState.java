package org.flasck.flas.typechecker;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.errors.ErrorResult;

public class TypeState {
	TypeEnvironment gamma;
	final PhiSolution phi;
	final Map<String, Object> localKnowledge;

	public TypeState(ErrorResult errors) {
		gamma = new TypeEnvironment();
		phi = new PhiSolution(errors);
		localKnowledge = new HashMap<String, Object>();
	}

}
