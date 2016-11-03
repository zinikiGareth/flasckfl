package org.flasck.flas.newtypechecker;

public class BindConstraint implements Constraint {
	private final TypeInfo ty;

	public BindConstraint(TypeInfo ty) {
		this.ty = ty;
	}

	@Override
	public String toString() {
		return "B[" + ty + "]";
	}
}
