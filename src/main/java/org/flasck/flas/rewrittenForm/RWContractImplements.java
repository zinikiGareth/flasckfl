package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.SolidName;

public class RWContractImplements extends RWImplements {
	public final String referAsVar;
	public final InputPosition varLocation;

	public RWContractImplements(InputPosition kw, InputPosition location, CSName csName, SolidName name, InputPosition vlocation, String referAsVar) {
		super(kw, location, csName, name);
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
