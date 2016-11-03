package org.flasck.flas.newtypechecker;

import org.flasck.flas.rewrittenForm.RWStructDefn;

public class SwitchConstraint implements Constraint {
	private final RWStructDefn ctor;

	public SwitchConstraint(RWStructDefn ctor) {
		this.ctor = ctor;
	}

	@Override
	public String toString() {
		return "Sw[" + ctor.name() + "]";
	}
}
