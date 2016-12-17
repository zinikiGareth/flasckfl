package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.VarName;

public class RWVarPattern implements Locatable {
	public final InputPosition varLoc;
	public final VarName var;

	public RWVarPattern(InputPosition varLoc, VarName vn) {
		this.varLoc = varLoc;
		this.var = vn; // jsName();
	}
	
	@Override
	public String toString() {
		return "RWV[" + var.jsName() + "]";
	}

	@Override
	public InputPosition location() {
		return varLoc;
	}
}
