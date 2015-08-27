package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class VarPattern {
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
}
