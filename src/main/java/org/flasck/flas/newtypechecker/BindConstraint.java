package org.flasck.flas.newtypechecker;

public class BindConstraint implements Constraint {
	public final TypeInfo ty;

	public BindConstraint(TypeInfo ty) {
		this.ty = ty;
	}

	@Override
	public TypeInfo typeInfo() {
		return ty;
	}

	@Override
	public String toString() {
		return "B[" + ty + "]";
	}
}
