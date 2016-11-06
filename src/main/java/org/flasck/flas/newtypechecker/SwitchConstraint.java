package org.flasck.flas.newtypechecker;

import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.zinutils.exceptions.NotImplementedException;

public class SwitchConstraint implements Constraint {
	private final RWStructDefn ctor;

	public SwitchConstraint(RWStructDefn ctor) {
		this.ctor = ctor;
	}

	@Override
	public TypeInfo typeInfo() {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return "Sw[" + ctor.name() + "]";
	}
}
