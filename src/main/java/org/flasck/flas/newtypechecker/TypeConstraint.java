package org.flasck.flas.newtypechecker;

public class TypeConstraint implements Constraint {
	private final TypeInfo ty;

	public TypeConstraint(TypeInfo ti) {
		this.ty = ti;
	}

	@Override
	public String toString() {
		return "T[" + ty + "]";
	}
}
