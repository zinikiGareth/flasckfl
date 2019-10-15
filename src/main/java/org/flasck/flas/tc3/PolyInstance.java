package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.parsedForm.StructDefn;
import org.zinutils.exceptions.NotImplementedException;

public class PolyInstance implements Type {
	private final StructDefn ty;
	private final List<Type> polys;

	public PolyInstance(StructDefn ty, List<Type> polys) {
		this.ty = ty;
		this.polys = polys;
	}

	public List<Type> getPolys() {
		return polys;
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		return 0;
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
		return ty.name().uniqueName() + polys;
	}
}
