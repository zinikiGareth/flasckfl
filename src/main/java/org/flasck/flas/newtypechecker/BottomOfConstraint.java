package org.flasck.flas.newtypechecker;

public class BottomOfConstraint implements Constraint {
	private final TypeInfo want;

	public BottomOfConstraint(TypeInfo want) {
		this.want = want;
	}

	@Override
	public String toString() {
		return "LessTopThan[" + want + "]";
	}
}
