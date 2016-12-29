package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.StructName;

public class RWContractImplements extends RWImplements {
	public final String referAsVar;
	public final InputPosition varLocation;

	public RWContractImplements(InputPosition kw, InputPosition location, StructName name, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTIMPL, name);
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
