package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.StructName;

public class RWContractImplements extends RWImplements {
	public final String referAsVar;
	public final InputPosition varLocation;

	public RWContractImplements(InputPosition kw, InputPosition location, CSName csName, StructName name, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTIMPL, csName, name);
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
