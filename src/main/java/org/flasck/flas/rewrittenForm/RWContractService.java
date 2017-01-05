package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.SolidName;

public class RWContractService extends RWImplements {
	public final String referAsVar;
	public final InputPosition vlocation;

	public RWContractService(InputPosition kw, InputPosition location, CSName csName, SolidName type, InputPosition vlocation, String referAsVar) {
		super(kw, location, csName, type);
		this.vlocation = vlocation;
		this.referAsVar = referAsVar;
	}
}
