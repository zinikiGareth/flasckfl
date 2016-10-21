package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class VarPattern implements Locatable {
	public final InputPosition varLoc;
	public final String var;

	public VarPattern(InputPosition varLoc, String text) {
		this.varLoc = varLoc;
		this.var = text;
	}
	
	@Override
	public String toString() {
		return var;
	}

	@Override
	public InputPosition location() {
		return varLoc;
	}
}
