package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class ContractService extends Implements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public ContractService(InputPosition location, String type, InputPosition vlocation, String referAsVar) {
		super(location, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
