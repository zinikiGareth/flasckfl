package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class ContractImplements extends Implements {
	public final String referAsVar;
	public final InputPosition varLocation;

	public ContractImplements(InputPosition kw, InputPosition location, TypeReference type, InputPosition vlocation, String referAsVar) {
		super(kw, location, type);
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
