package org.flasck.flas.newtypechecker;

public class TypeConstraint implements Constraint {
	private final String ty;

	public TypeConstraint(String ty) {
		this.ty = ty;
	}

	@Override
	public String toString() {
		return "T[" + ty + "]";
	}
}
