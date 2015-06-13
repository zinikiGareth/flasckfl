package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class ContractImplements extends Implements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public ContractImplements(InputPosition location, String type, InputPosition vlocation, String referAsVar) {
		super(location, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
