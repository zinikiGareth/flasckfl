package org.flasck.flas.newtypechecker;

public class TypeConstraint implements Constraint {
	public final TypeInfo ty;

	public TypeConstraint(TypeInfo ti) {
		this.ty = ti;
	}
	
	@Override
	public TypeInfo typeInfo() {
		return ty;
	}

	@Override
	public String toString() {
		return "T[" + ty + "]";
	}
}
