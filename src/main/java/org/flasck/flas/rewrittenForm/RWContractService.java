package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.StructName;

public class RWContractService extends RWImplements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public RWContractService(InputPosition kw, InputPosition location, CSName csName, StructName type, InputPosition vlocation, String referAsVar) {
		super(kw, location, WhatAmI.CONTRACTSERVICE, csName, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
