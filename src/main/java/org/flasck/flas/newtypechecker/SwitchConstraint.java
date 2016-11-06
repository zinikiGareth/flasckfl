package org.flasck.flas.newtypechecker;

import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.zinutils.exceptions.NotImplementedException;

public class SwitchConstraint implements Constraint {
	private final NamedType ctor;

	public SwitchConstraint(NamedType ty) {
		this.ctor = ty;
	}

	@Override
	public TypeInfo typeInfo() {
		return ctor;
	}

	@Override
	public String toString() {
		return "Sw[" + ctor.name + "]";
	}
}
