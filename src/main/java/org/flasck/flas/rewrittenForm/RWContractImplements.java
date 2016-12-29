package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class RWContractImplements extends RWImplements {
	public final String referAsVar;
	public final InputPosition varLocation;

	public RWContractImplements(InputPosition kw, InputPosition location, NameOfThing name, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTIMPL, name.uniqueName());
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
