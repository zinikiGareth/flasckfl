package org.flasck.flas.tc3;

import java.util.Map;
import java.util.TreeMap;

public class FunctionGroupTCState implements CurrentTCState {

	private Map<String, TypeConstraintSet> constraints = new TreeMap<>();

	@Override
	public UnifiableType functionParameter(String var) {
		if (constraints.containsKey(var))
			return constraints.get(var);
		TypeConstraintSet ret = new TypeConstraintSet();
		constraints .put(var, ret);
		return ret;
	}

	@Override
	public UnifiableType hasVar(String var) {
		return constraints.get(var);
	}

}
