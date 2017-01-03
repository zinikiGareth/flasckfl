package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.StructName;

public class RWContractService extends RWImplements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public RWContractService(InputPosition kw, InputPosition location, StructName type, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTSERVICE, null, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
