package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class RWContractService extends RWImplements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public RWContractService(InputPosition kw, InputPosition location, String type, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTSERVICE, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
