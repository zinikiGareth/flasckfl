package org.flasck.flas.tc3;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.RepositoryReader;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionGroupTCState implements CurrentTCState {
	private final RepositoryReader repository;
	private final Map<String, TypeConstraintSet> constraints = new TreeMap<>();
	int polyCount = 0;
	
	public FunctionGroupTCState(RepositoryReader repository) {
		this.repository = repository;
	}

	@Override
	public UnifiableType nextArg() {
		return new TypeConstraintSet(repository, this, null);
	}

	@Override
	public void argType(Type ty) {
//		throw new NotImplementedException();
	}

	@Override
	public UnifiableType requireVarConstraints(InputPosition pos, String var) {
		if (constraints.containsKey(var))
			return constraints.get(var);
		TypeConstraintSet ret = new TypeConstraintSet(repository, this, pos);
		constraints.put(var, ret);
		return ret;
	}

	@Override
	public UnifiableType hasVar(String var) {
		return constraints.get(var);
	}

	@Override
	public PolyType nextPoly(InputPosition pos) {
		if (polyCount >= 26)
			throw new NotImplementedException("Cannot handle more than 26 poly types at once");
		return new PolyType(pos, new String(new char[] { (char)('A' + polyCount++) }));
	}
}
