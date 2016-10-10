package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class ContractImplements extends Implements {
	public final String referAsVar;
	public final InputPosition varLocation;

	public ContractImplements(InputPosition kw, InputPosition location, String type, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTIMPL, type);
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
