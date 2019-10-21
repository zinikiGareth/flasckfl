package org.flasck.flas.tc3;

import java.util.List;

import org.zinutils.exceptions.NotImplementedException;

public class ConsolidateTypes implements Type {
	public final List<Type> types;

	public ConsolidateTypes(List<Type> types) {
		this.types = types;
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException();
	}
	
	@Override
	public String toString() {
		return "Consolidate" + types;
	}
}
