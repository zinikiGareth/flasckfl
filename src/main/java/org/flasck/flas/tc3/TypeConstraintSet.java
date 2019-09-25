package org.flasck.flas.tc3;

import java.util.HashSet;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.NotImplementedException;

public class TypeConstraintSet implements UnifiableType {
	private final CurrentTCState state;
	private final Set<Type> incorporatedBys = new HashSet<>();
	private final InputPosition pos;
	private Type t;
	
	public TypeConstraintSet(CurrentTCState state, InputPosition pos) {
		this.state = state;
		this.pos = pos;
	}

	@Override
	public Type resolve() {
		if (incorporatedBys.isEmpty())
			t = state.nextPoly(pos);
		else {
			// TODO: merge multiple things or throw an error
			t = incorporatedBys.iterator().next();
		}
		return t;
	}
	
	@Override
	public void incorporatedBy(InputPosition pos, Type incorporator) {
		incorporatedBys.add(incorporator);
	}

	@Override
	public String signature() {
		if (t == null)
			throw new NotImplementedException("Has not been resolved");
		return t.signature();
	}

	@Override
	public int argCount() {
		if (t == null)
			throw new NotImplementedException("Has not been resolved");
		return t.argCount();
	}

	@Override
	public Type get(int pos) {
		if (t == null)
			throw new NotImplementedException("Has not been resolved");
		return t.get(pos);
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException("The type algorithm should recognize us and call incorporatedBy instead");
	}

}
