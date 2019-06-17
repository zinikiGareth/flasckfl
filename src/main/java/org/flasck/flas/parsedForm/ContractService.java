package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;

public class ContractService extends Implements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public ContractService(InputPosition kw, InputPosition location, TypeReference type, CSName csn, InputPosition vlocation, String referAsVar) {
		super(kw, location, type, csn);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
