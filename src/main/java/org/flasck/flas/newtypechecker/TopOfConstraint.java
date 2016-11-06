package org.flasck.flas.newtypechecker;

public class TopOfConstraint implements Constraint {
	public final TypeInfo have;

	public TopOfConstraint(TypeInfo have) {
		this.have = have;
	}
	
	@Override
	public TypeInfo typeInfo() {
		return have;
	}

	@Override
	public String toString() {
		return "MoreTopThan[" + have + "]";
	}
}
