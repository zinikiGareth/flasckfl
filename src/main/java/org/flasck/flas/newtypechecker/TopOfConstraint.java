package org.flasck.flas.newtypechecker;

public class TopOfConstraint implements Constraint {
	private final TypeInfo have;

	public TopOfConstraint(TypeInfo have) {
		this.have = have;
	}

	@Override
	public String toString() {
		return "MoreTopThan[" + have + "]";
	}
}
