package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;

public class ContractService extends Implements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public ContractService(InputPosition kw, InputPosition location, String type, InputPosition vlocation, String referAsVar) {
		super(kw, location, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	public CSName getRealName() {
		return (CSName) realName;
	}
}
