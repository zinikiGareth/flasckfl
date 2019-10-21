package org.flasck.flas.tc3;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.RepositoryReader;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionGroupTCState implements CurrentTCState {
	private final RepositoryReader repository;
	private final Map<String, UnifiableType> constraints = new TreeMap<>();
	int polyCount = 0;
	private Set<UnifiableType> allUTs = new HashSet<>();
	
	public FunctionGroupTCState(RepositoryReader repository) {
		this.repository = repository;
	}

	@Override
	public UnifiableType createUT() {
		TypeConstraintSet ret = new TypeConstraintSet(repository, this, null);
		allUTs.add(ret);
		return ret;
	}

	@Override
	public void argType(Type ty) {
//		throw new NotImplementedException();
	}

	@Override
	public void bindVarToUT(String name, UnifiableType ty) {
		if (!allUTs.contains(ty))
			throw new NotImplementedException("Where did this come from?");
		constraints.put(name, ty);
	}

	@Override
	public UnifiableType requireVarConstraints(InputPosition pos, String var) {
		if (!constraints.containsKey(var))
			throw new RuntimeException("We don't have var constraints for " + var + " but it should have been bound during arg processing");
		return constraints.get(var);
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
	
	@Override
	public void resolveAll() {
		for (UnifiableType ut : allUTs) {
			ut.resolve();
		}
	}
}
